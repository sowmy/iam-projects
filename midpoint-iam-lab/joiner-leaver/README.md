# Joiner / Leaver: HR-driven provisioning, source to IGA to directory

This is the full pipeline working end to end. Someone gets hired in HR, midPoint creates their identity, and they get a directory account in OpenLDAP without anyone touching LDAP by hand. HR stays the only place employment status is decided, and everything downstream follows from it.

```
SimplifyHR (hr.csv)  ──inbound──▶  midPoint user  ──Employee role──▶  OpenLDAP account
   system of record                  identity                          uid=<empid>,ou=people,dc=idpnode,dc=net
```

## How it works

The first half is inbound. midPoint reads each HR record, correlates it on employee ID so nobody gets duplicated, and builds the user from it. That includes generating the email and setting enabled or disabled from the HR status.

The second half is outbound. Outbound mappings do the reverse of inbound ones: they take properties of the midPoint user and write them onto the LDAP account. Given name, surname, a `cn` built from both, email, cost center as `departmentNumber`, and the employee ID as `employeeNumber`. They're all strong, so if someone edits an entry directly in LDAP, midPoint puts it back on the next sync.

Every LDAP entry needs a distinguished name (DN), which is its full path in the directory tree. I build it with a small Groovy script: `uid=` plus the employee ID, placed under `ou=people`. The script also has a branch that points a disabled user's DN to `ou=inactive`. In practice that branch never fires: when someone is disabled, the `Employee` role drops off and midPoint deletes the LDAP account outright (see the leaver test below).

The account itself comes from a role, not from a rule on the resource. I created an `Employee` business role whose inducement says "members get an OpenLDAP account". midPoint assigns that role automatically to every user who isn't disabled. So a new hire gets the account without anyone clicking anything, and the access is tied to a reason (being an employee) that you can review later. That's the difference between role-based provisioning and just pushing every user everywhere.

## Screenshots

![Users page with two accounts each](users-two-accounts.jpg)

All seven employees now have 2 accounts each: the HR record they came from, and the LDAP account midPoint created for them.

![User 1001 projections](user-1001-projections.jpg)

User 1001's linked accounts: the SimplifyHR record and the OpenLDAP entry at `uid=1001,ou=people,dc=idpnode,dc=net`, with the DN built by the script.

![Employee role inducement](employee-role-inducement.jpg)

The `Employee` business role. Its one inducement is an OpenLDAP account, so membership in the role is what gives someone a directory account.

![LDAP tree under ou=people](ldap-people-tree.png)

The directory tree in phpLDAPadmin: `ou=people` now holds `uid=1001` to `uid=1007`, one for each employee, all created by midPoint. The eighth entry, `uid=administrator`, is midPoint's built-in admin. It got an LDAP account because the `Employee` role is auto-assigned to every enabled user, admin included. Narrowing that rule to HR-sourced users would keep accounts like this out of the people branch.

![LDAP entry for uid=1001](ldap-people-entries.jpg)

Sophie Müller's entry (`uid=1001`) up close, showing the values that came all the way from HR: `cn`, `departmentNumber`, email, `employeeNumber` and `givenName`.

![Resources](ldap-resources.jpg)

Both ends of the pipeline as midPoint resources: SimplifyHR as the source (CSV connector) and OpenLDAP as the target (LDAP connector 3.9.4).

![midPoint Users page](users-hr-identities.jpg)

The first stage on its own: the seven identities midPoint created from HR records, before LDAP was connected.

![Audit log filtered to today](audit-log-today.jpg)

The audit log filtered to the day of the HR reconciliation run, so every change midPoint made can be traced back to the sync that caused it.

## Testing the lifecycle: joiner and leaver

To prove the pipeline works both ways, I drove a joiner and a leaver through HR and let midPoint do the rest. I didn't edit LDAP or midPoint by hand at any point.

**Joiner.** I added a new hire in SimplifyHR, Joy Banala (`1008`), and ran the HR reconciliation. midPoint created user `1008` with the email `joy.banala@idpnode.net`, auto-assigned the `Employee` role, and created `uid=1008,ou=people,dc=idpnode,dc=net` in LDAP, all within about two seconds.

![LDAP after the joiner](ldap-after-joiner.png)

After the joiner: all eight employees (`uid=1001` to `uid=1008`) are under `ou=people`, plus `uid=administrator`, so phpLDAPadmin shows 9.

**Leaver.** Then I changed Oliver Bennett's (`1006`) status in SimplifyHR so he was no longer `Active`, and reconciled again. The status mapping disabled user `1006` in midPoint, the auto-assigned `Employee` role dropped off, and midPoint deleted his LDAP account. He's still in midPoint as a disabled user with his HR account linked, so his history is kept, but he has no directory access. I ran the leaver twice: once on its own, and once more after re-enabling him to capture the before and after. Both runs behaved the same way.

![LDAP after the leaver](ldap-after-leaver.png)

After the leaver: `uid=1006` is gone. `ou=people` holds the seven current employees (`1001` to `1005`, `1007`, `1008`) plus `uid=administrator`, so phpLDAPadmin shows 8.

### The audit trail

Every step is in midPoint's audit log, and each change is tagged with the reconciliation channel. That shows it was driven by the HR sync and not by someone clicking in midPoint.

![Audit log for the joiner and leaver](audit-joiner-leaver.jpg)

Newest at the top. Times are UTC on 15 November.

| Time | Event | What it means |
|---|---|---|
| 20:37:20 | Add object `1008` and Resource: Add object | The joiner: user `1008` was created, then his LDAP account |
| 20:49:25 | Modify object `1006`, then Resource: Delete object `uid=1006,ou=people,…` | The first leaver run: Oliver was disabled and his LDAP account was removed |
| 21:19:54 | Modify object `1006` and Resource: Add object | Oliver was re-enabled in HR, and his LDAP account was created again |
| 21:22:23 | Modify object `1006`, then Resource: Delete object `uid=1006,ou=people,…` | The second leaver run: disabled again and the account was removed again |

Each change comes straight after a "Run task immediately: SimplifyHR Reconciliation" entry. That's the point for a real organisation: an auditor can see who joined or left, what access was granted or removed, exactly when, and that HR triggered it.

## The config behind it

**HR to midPoint (inbound)**
- [`simplifyhr-resource.xml`](../config/simplifyhr-resource.xml): the HR resource, with inbound mappings, correlation on `empid`, and delete turned off so midPoint can never remove anything from HR.
- [`email-generation.groovy`](../config/scripts/email-generation.groovy): builds `first.last@idpnode.net` and strips accents, so Müller becomes `muller`.
- [`status-to-activation.groovy`](../config/scripts/status-to-activation.groovy): the leaver control. Any HR status other than `Active` disables the user.

**midPoint to LDAP (outbound)**
- [`outbound-mappings.xml`](../config/outbound-mappings.xml): the outbound mappings from the LDAP resource, showing what each LDAP attribute is built from, plus correlation. I trimmed it down to the part I designed and left out the connector settings and the bind password.
- [`ldap-dn.groovy`](../config/scripts/ldap-dn.groovy): the DN script, `uid=<empid>` under `ou=people`, or `ou=inactive` for disabled users.
- [`employee-role.xml`](../config/employee-role.xml): the `Employee` role, with the LDAP account inducement and the auto-assignment for every user who isn't disabled.

The full build for the HR side is in the [walkthrough](../docs/walkthrough.md).
