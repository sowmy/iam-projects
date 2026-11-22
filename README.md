# Enterprise IAM Implementation Lab

This is where I keep my IAM lab work. I'm using a local lab environment (midPoint, an HR app and OpenLDAP) to work through the identity use cases you'd see in a real company: onboarding people from HR, provisioning their accounts, and cleaning up when they leave.

The environment itself came from a lab bundle. The configuration on top of it is mine, and that's what this repo documents: the configs I wrote, why I set things up the way I did, and screenshots of it working.

## Use cases

I'm adding these as I go, so the status column is honest about where each one is.

| Use case | Platform | Status |
|---|---|---|
| [HR-driven provisioning, source to IGA to directory (joiner / leaver)](./midpoint-iam-lab/joiner-leaver/) | SimplifyHR → midPoint → OpenLDAP | ✅ Done |
| [Non-human identities: service account owners and orphan detection](./midpoint-iam-lab/nhi-ownership/) | OpenLDAP → midPoint | ✅ Done |
| Mover: department change drives access change | midPoint | ⏳ Planned |
| [RBAC: department birthright roles and a requested role](./midpoint-iam-lab/rbac/) | midPoint → OpenLDAP | ✅ Done (open points noted) |
| SSO federation (OIDC / SAML) | Auth0 | ⏳ Planned |

## What I'm working with

- **midPoint** is the IGA platform. It holds the identities and runs the mappings, correlation and reconciliation.
- **SimplifyHR** is a small Flask app that plays the HR system. It writes employee records to an `hr.csv` file, and midPoint reads that same file through its CSV connector.
- **OpenLDAP** (`dc=idpnode,dc=net`) is the directory midPoint provisions accounts into, through a role-based LDAP resource.
- **Auth0** is for the SSO part, which I haven't started yet.

## Labs

- [midPoint IAM lab](./midpoint-iam-lab/) has the architecture, my full build walkthrough, the configs, and what I learned along the way.
