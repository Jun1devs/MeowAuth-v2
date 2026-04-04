# 🔐 Security Policy

Thank you for helping keep MeowAuth and the Minecraft community safe. We take security vulnerabilities seriously.

## 📦 Supported Versions

We only provide security updates for the latest major release. Please upgrade before reporting issues.

| Version | Supported |
|---------|-----------|
| 2.0.x   | ✅ Yes     |
| < 2.0   | ❌ No      |

 ## 🚨 Reporting a Vulnerability

**DO NOT** report security vulnerabilities via public GitHub Issues, Discord, or forums. Public disclosure before a fix puts all server owners at risk.

Instead, use one of these secure channels:
1. **GitHub Security Advisories** (Recommended): Go to the [Security tab](https://github.com/Jun1devs/MeowAuth-v2/security/advisories/new) and click `Report a vulnerability`.
2. **Direct Email**: If GitHub Advisories aren't available, email us at `[YOUR-EMAIL]@gmail.com` (замените на вашу почту).

## 📝 What to Include
To help us investigate quickly, please provide:
- Minecraft & Forge versions
- MeowAuth version
- Detailed steps to reproduce the vulnerability
- Server/Client logs (`latest.log`, `debug.log`)
- Proof of concept (screenshots, video, or code snippet)
- Impact assessment (what can an attacker do?)

## ⏱️ Response Timeline
- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 5 business days
- **Patch Release**: 7–14 days for Critical/High severity
- **Public Disclosure**: Only after a patched version is published

## 🛡️ Responsible Disclosure
- We will credit you in the release notes and `SECURITY.md` unless you request anonymity.
- Please allow us reasonable time to fix the issue before publishing any details.
- Do not test exploits on public servers without explicit owner permission.

## 🔒 Our Security Commitments
- All tokens are hashed with **BCrypt** (cost factor 12)
- Rate limiting & brute-force protection enabled by default
- Zero plain-text token/password storage
 - Regular dependency audits & updates

---
*Last updated: April 2026*