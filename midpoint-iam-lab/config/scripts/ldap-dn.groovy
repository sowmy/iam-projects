// Outbound mapping: midPoint user -> LDAP distinguished name (ri:dn)
// Source: user's name (= HR empid). Builds uid=<empid>,ou=...,dc=idpnode,dc=net
// Active users go under ou=people; disabled users are placed under ou=inactive.
// Example: user 1001 -> uid=1001,ou=people,dc=idpnode,dc=net

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType

if (user?.activation?.administrativeStatus == ActivationStatusType.DISABLED) {
    return 'uid=' + name + ',ou=inactive,dc=idpnode,dc=net'
} else {
    return 'uid=' + name + ',ou=people,dc=idpnode,dc=net'
}
