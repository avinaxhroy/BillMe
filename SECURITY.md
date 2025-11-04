# Security Policy

## 🔐 Reporting Security Vulnerabilities

At BillMe, we take security seriously. If you discover a security vulnerability, please follow these guidelines:

### How to Report

**Please do NOT open a public issue for security vulnerabilities.** Instead:

1. **Email us directly** at [security contact - add your email]
2. **Include the following details:**
   - Type of vulnerability
   - Location of affected code (if applicable)
   - Step-by-step reproduction instructions
   - Potential impact assessment
   - Suggested fix (if you have one)

### What to Expect

- We will acknowledge receipt of your report within **48 hours**
- We will investigate and provide status updates every **72 hours**
- We will work with you to understand and resolve the issue
- We will credit you in security advisories (unless you prefer anonymity)

## 🛡️ Supported Versions

| Version | Supported          | Notes |
|---------|-------------------|-------|
| 1.0.x   | ✅ Yes            | Current release |
| < 1.0   | ❌ No             | Pre-release development |

## 🔒 Security Best Practices

### For Contributors

- **Never commit secrets** (API keys, passwords, tokens) to the repository
- Use `.gitignore` to exclude sensitive files
- Follow OWASP guidelines for secure coding
- Report vulnerabilities privately, not in issues/PRs
- Keep dependencies updated

### For Users

- **Always use official releases** from [GitHub Releases](https://github.com/avinaxhroy/BillMe/releases)
- **Verify signatures** on released APKs
- **Keep Android updated** to latest security patches
- **Enable ProGuard/R8** in production builds
- **Use HTTPS** for any network communications
- **Restrict file permissions** on device storage

## 🚨 Known Security Considerations

### IMEI Validation
- IMEI validation uses the Luhn algorithm for format validation only
- Not a cryptographic guarantee of device authenticity
- Should be combined with other validation methods in production

### Offline Storage
- Local SQLite database is stored on device
- For sensitive data, implement encryption at rest
- Consider using Android Keystore for sensitive keys

### Camera/Hardware Access
- Camera requires `android.permission.CAMERA`
- Bluetooth/Serial requires appropriate permissions
- Always request permissions at runtime on Android 6.0+

### Backup & Export
- Backup files may contain sensitive business data
- Implement backup encryption for production use
- Secure backup transmission and storage

## 🔄 Security Updates

- Security patches will be released as soon as possible after confirmation
- Updates will be tagged with security notices
- Critical vulnerabilities will trigger immediate releases
- Users are encouraged to update as soon as patches are available

## 🔍 Security Audit

This project currently:
- ✅ Uses HTTPS for all external communications
- ✅ Implements input validation
- ✅ Follows Android security guidelines
- ✅ Uses Hilt for dependency injection (safer than manual DI)
- ⏳ Needs penetration testing before production use

### Recommendations for Production

Before deploying to production:

1. **Code Review**: Have security-focused code review
2. **Dependency Audit**: Run `./gradlew dependencyUpdates` to check for known vulnerabilities
3. **OWASP Testing**: Perform OWASP Mobile Top 10 security testing
4. **Encryption**: Implement data encryption at rest
5. **API Security**: Use certificate pinning for API communication
6. **Obfuscation**: Enable and test ProGuard/R8 configuration
7. **Permissions**: Minimize and justify all permission requests
8. **Testing**: Add security-focused unit and integration tests

## 🔗 Security Resources

- [Android Security & Privacy Guidelines](https://developer.android.com/topic/security)
- [OWASP Mobile Top 10](https://owasp.org/www-project-mobile-top-10/)
- [Google Play Security Best Practices](https://developer.android.com/google-play/policies/general-policies)
- [Kotlin Security Guidelines](https://kotlinlang.org/docs/security.html)

## 📞 Contact

For security inquiries:
- 🔒 **Email**: [Add your security contact email]
- 📌 **Discussions**: [GitHub Discussions](https://github.com/avinaxhroy/BillMe/discussions)
- 🐛 **Non-security Issues**: [GitHub Issues](https://github.com/avinaxhroy/BillMe/issues)

---

**Last Updated**: November 2025  
**Policy Version**: 1.0

Thank you for helping keep BillMe secure! 🙏
