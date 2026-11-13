// Inbound mapping: HR "status" column -> midPoint activation/administrativeStatus
// Anything other than "Active" (e.g. Terminated, Leave) disables the identity.
// This is the "leaver" control: HR changes status, midPoint disables the user.

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType

if (input == 'Active') {
    return ActivationStatusType.ENABLED
} else {
    return ActivationStatusType.DISABLED
}
