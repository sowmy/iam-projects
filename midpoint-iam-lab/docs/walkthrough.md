# Walkthrough: how I built the HR → midPoint flow

These are the steps I followed to get HR records flowing into midPoint. The screenshots I mention are in [`../screenshots`](../screenshots).

## What I was working with

| Service | Where | Notes |
|---|---|---|
| midPoint | `http://localhost:8080/midpoint` | The admin console |
| phpLDAPadmin | `http://localhost:8089` | Logged in as `cn=admin,dc=idpnode,dc=net` |
| HR export | `/opt/midpoint/var/import/hr.csv` | The path as midPoint sees it |

## Step 1: Get the HR file in place

The HR export needs to be at `/opt/midpoint/var/import/hr.csv`. The format is in [`sample-data/hr.csv`](../sample-data/hr.csv), and it has these columns: `empid, firstname, lastname, department, costcenter, manager, status`.

## Step 2: Create the SimplifyHR resource

1. I went to **Resources → New resource → From scratch** and picked the **CSV connector** (v2.9).
2. I set the file path to `/opt/midpoint/var/import/hr.csv`, the delimiter to `,`, and made `empid` both the name attribute and the unique attribute.
3. I ran schema discovery. midPoint found `AccountObjectClass` with all 7 columns.
4. I clicked **Test connection** and made sure every check came back green (`04-test-connection.jpg`).

## Step 3: Define the account type

I set kind `ACCOUNT`, intent `default`, object class `AccountObjectClass`, and focus type `UserType`.

## Step 4: Add the inbound mappings

I added one inbound mapping for each attribute. The full list is in the table in the README. Two of them needed Groovy scripts:

- **Email:** [`email-generation.groovy`](../config/scripts/email-generation.groovy)
- **Status to activation:** [`status-to-activation.groovy`](../config/scripts/status-to-activation.groovy)

## Step 5: Set up correlation and sync

- I correlated on the `name` item.
- For the reactions I used `unmatched → addFocus`, `unlinked → link`, and `linked → synchronize`.

## Step 6: Protect the source

Under the resource's capabilities, I turned off **Delete** so midPoint can never remove anything from HR.

## Step 7: Reconcile

I went to **Resource → Defined tasks → Create reconciliation task** (kind Account, intent default) and ran it. Then I checked three things:

- The task showed **Success** with 7 objects processed (`11-reconciliation-task.jpg`).
- Under **Resource → Accounts**, every row showed `LINKED` (`05-hr-accounts-linked.jpg`).
- The **Users** list showed the new people with their generated emails (`07-users-list.jpg`).

## Step 8: Check one user properly

I opened user `1001` to make sure everything had come through:

- On the **Basic** tab, the name, given and family names, email, cost center and org unit all came from HR (`08-user-1001-attributes.jpg`).
- Under **Projections**, there was exactly one SimplifyHR account (`09-user-1001-projection.jpg`).

## Phase 2: LDAP

After this I connected OpenLDAP (`dc=idpnode,dc=net`, `12-ldap-directory.jpg` shows the empty base) as a second resource with outbound mappings, and created an `Employee` role that gives every enabled user an account under `ou=people`. That half of the pipeline, with its config and screenshots, is written up in [joiner-leaver](../joiner-leaver/).
