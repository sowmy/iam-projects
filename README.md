# Enterprise IAM Implementation Lab

I built this complete Identity and Access Management (IAM) environment from scratch to replicate how IAM provisioning works in a real enterprise engagement. The entire stack runs on a dedicated cloud server.

I put this repository together to document my configurations, architectural decisions, and screenshots from the build. It serves as a practical portfolio of my hands-on IAM implementation skills.

## The Environment

Here is the tech stack I used for this lab:

| Component | Purpose in the Lab |
| :--- | :--- |
| **midPoint** | IGA platform — Handles the identity lifecycle, provisioning, and reconciliation. |
| **SimplifyHR** | HR source of truth — A Python/Flask app I used to simulate an enterprise HRIS. |
| **OpenLDAP** | Target directory — Where user accounts and groups are actually provisioned. |
| **Auth0** | Access management — Handles OIDC and SAML federation. |

## What I Built

I wired these systems together to handle standard enterprise use cases:

* **JML (Joiner, Mover, Leaver) Automation:** Configured midPoint to automatically create, update, or disable directory accounts based on state changes in the HR system.
* **Downstream Provisioning:** Built inbound and outbound mappings to flow identity attributes smoothly from HR, through midPoint, and into OpenLDAP.
* **Identity Reconciliation:** Set up scheduled reconciliation tasks to catch and remediate any out-of-band changes made directly in LDAP.
* **SSO Integration:** Hooked up Auth0 to handle authentication using standard federation protocols.

## Documentation & Configs

I broke the build process down into distinct sessions. You'll find my XML/JSON configurations, screenshots, and setup notes in the links below:

* [Session 1: Infrastructure & HR Source Integration](./docs/session-1.md)
* [Session 2: midPoint Configuration & Resource Mapping](./docs/session-2.md)
* [Session 3: OpenLDAP Provisioning & Outbound Rules](./docs/session-3.md)
* [Session 4: Auth0 Setup & Federation](./docs/session-4.md)
