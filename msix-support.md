# Plan: MSIX installer support

## Goal

Add Windows-only MSIX packaging to the Beryx Badass JLink plugin. The new
packaging path will reuse the app image produced by the existing
`jpackageImage` task, stage it with an MSIX manifest and visual assets, pack
the staged directory with the Windows SDK `makeappx.exe` tool, and optionally
sign the result with `signtool.exe`.

The initial release should produce a sideloadable `.msix` package and also be
suitable for Microsoft Store submission when the project supplies the identity
reserved in Partner Center. It must not require code signing: unsigned output
is a valid result for Partner Center upload and for signing in a later CI
stage.

## Scope and constraints

- Build on Windows only. The task must fail early with an actionable message
  on other operating systems.
- Require `makeappx.exe`, supplied by the Windows SDK. `signtool.exe` is only
  required when signing is enabled.
- Support Windows `x64` and `arm64` packages. The configured package
  architecture must match the architecture of the app image; no
  cross-packaging is implied.
- Build from the directory emitted by `jpackageImage`; do not invoke a second
  `jpackage` run or modify its output in place.
- Keep existing `jpackage` installer types and behaviour unchanged.
- Treat the Microsoft Store's *Publisher ID* as Store metadata, not as a
  substitute for the manifest `Identity Publisher` value. Store-mode inputs
  must use the exact reserved package identity values supplied by Partner
  Center.

## Proposed public Gradle API

Add an `msix` block to the existing `jlink` extension, plus a dedicated
`jpackageMsix` task that depends on `jpackageImage`.

```groovy
jlink {
    jpackage {
        imageName = 'Acme Desk'
        appVersion = '2.4.0'
    }

    msix {
        packageName = 'Acme.Desk'                 // MSIX Identity Name
        publisher = 'CN=Acme Corporation, O=Acme Corporation, C=US'
        version = '2.4.0'                         // converted/validated as a.b.c.d
        architecture = 'x64'                      // x64 or arm64
        displayName = 'Acme Desk'
        description = 'Desktop client for Acme'
        vendor = 'Acme Corporation'
        icon = file('src/main/packaging/app.ico')
        capabilities = []                         // e.g. ['internetClient']

        // Optional. These values must be copied exactly from Partner Center.
        store {
            packageName = '12345Acme.AcmeDesk'
            publisher = 'CN=01234567-89AB-CDEF-0123-456789ABCDEF'
            publisherId = '01234567-89AB-CDEF-0123-456789ABCDEF'
        }

        // Optional: omitting this block leaves the .msix unsigned.
        signing {
            certificateFile = file('signing/acme.pfx')
            certificatePassword = providers.environmentVariable('MSIX_CERT_PASSWORD')
            timestampServer = 'http://timestamp.digicert.com'
        }
    }
}
```

Details to settle during implementation:

- Expose sensitive values as Gradle `Provider<String>` inputs and ensure they
  never appear in task logging, build scans, or generated files. Consider a
  certificate-thumbprint/store signing alternative after the PFX path is
  complete.
- Give all metadata sensible conventions from existing `jpackage` properties
  where safe (`imageName`, `appVersion`, and `vendor`), but require
  `packageName` and `publisher` unless Store mode supplies them.
- Provide a deterministic output convention such as
  `build/jpackage-msix/<installerName>-<version>-<architecture>.msix` and a
  separate staging directory under `build/`.
- Make the task's output file, staging directory, inputs, and tool locations
  Gradle properties so incremental execution and configuration cache behaviour
  are correct. It is acceptable for the external packaging task itself to be
  non-cacheable initially.

## Implementation plan

1. Model the configuration.

   Create `MsixData`, with nested `MsixStoreData` and `MsixSigningData`, using
   Gradle `Property`, `ListProperty`, `RegularFileProperty`, and
   `DirectoryProperty` types consistent with `JPackageData`. Add it to
   `JlinkPluginExtension`, including Groovy-DSL action methods. Mark metadata,
   assets, app-image directory, and signing mode as task inputs; mark the PFX
   as a sensitive input file and the password `@Internal`.

2. Register and wire the task.

   Add a `JPackageMsixTask`/implementation pair and register it in
   `JlinkPlugin`. It depends on `jpackageImage`, consumes that image directory,
   and writes only beneath its own output directory. Give it a clear
   description and an `onlyIf`/validation strategy that reports unsupported
   hosts, invalid architectures, missing required metadata, and missing SDK
   executables before staging begins.

3. Resolve Windows SDK tools robustly.

   Allow explicit `makeAppxPath` and `signToolPath` overrides for local and CI
   builds. Otherwise locate the newest compatible SDK installation from the
   standard Windows Kits location (and/or `PATH`), preferring the host tool
   architecture. Test this resolver independently and make failure messages
   name the expected executables and configuration override.

4. Stage the package layout.

   Copy the complete `jpackageImage` directory into a fresh staging directory
   without altering the original. Generate `AppxManifest.xml` at staging root
   and copy generated/provided images to the manifest-referenced paths. Use
   XML escaping and a fixed UTF-8 encoding; do not construct XML by unescaped
   string interpolation.

   The initial manifest should include:

   - `Identity` (`Name`, `Publisher`, `Version`, `ProcessorArchitecture`)
   - `Properties` (`DisplayName`, `PublisherDisplayName`, `Description`,
     `Logo`)
   - `Resources` with the appropriate language qualifier strategy
   - one full-trust `Application`, pointing to the launcher `.exe` in the app
     image and using `Windows.FullTrustApplication`
   - `uap:VisualElements` with display name, description, background colour,
     square/wide logos, and a store logo
   - requested capabilities using the correct manifest schema namespace

   Launcher selection needs an explicit rule: default to the primary launcher
   derived from `jpackage.imageName`, allow an `executable` override, and fail
   if the selected executable is absent. Secondary launchers are not separate
   MSIX applications in the first version.

5. Handle visual assets.

   Define the required initial asset set and the expected paths, including at
   least Square44x44, Square150x150, Wide310x150, and StoreLogo PNG assets at
   the required scales. Accept a directory of ready-made assets as the
   baseline. If a single application icon is configured, generate the required
   PNG variants into staging (with documented image-quality constraints) and
   retain an override for hand-crafted branded assets. Validate every
   manifest-referenced file before packing.

6. Pack and validate.

   Invoke `makeappx pack /d <staging-dir> /p <output.msix> /o`. Then invoke
   `makeappx validate /p <output.msix>` by default, with a configuration switch
   for environments that must skip validation. Capture tool output and surface
   it in Gradle failures without exposing secrets.

7. Sign when configured.

   With `signing.enabled`/a certificate configured, invoke `signtool sign`
   using SHA-256, the PFX, its password, and the optional RFC 3161 timestamp
   server. Validate signing inputs together (certificate, password, and tool)
   and delete no user files. Keep the unsigned artifact when signing is
   disabled; describe that this is the expected artifact for Partner Center
   upload or external signing.

8. Add automated coverage.

   Unit-test configuration defaults, validation, Store-mode precedence,
   version conversion/validation, XML generation and escaping, launcher
   selection, asset discovery, and command-line construction. Add Windows-only
   functional tests using a small fixture app and fake SDK tool executables for
   deterministic task wiring tests. Add a separately gated real-tool smoke
   test when GitHub-hosted runner SDK availability is confirmed.

9. Document and publish.

   Extend the user guide with the `msix` DSL, identity rules, asset matrix,
   unsigned/signing workflows, Store-mode requirements, and Windows-only
   limitation. Add a complete GitHub Actions example that runs on
   `windows-latest`, sets up the intended JDK, verifies `makeappx.exe` and
   `signtool.exe`, runs `jpackageMsix`, and uploads the `.msix` artifact.

## Acceptance criteria

- On a `windows-latest` runner with Windows SDK tools installed,
  `gradlew jpackageMsix` creates a valid x64 `.msix` from a standard
  `jpackageImage` output.
- The produced package passes `makeappx validate` and contains the app image,
  root manifest, and every referenced visual asset.
- `arm64` configuration generates an arm64 manifest and rejects incompatible
  inputs.
- A configuration with no signing block succeeds and emits an unsigned MSIX.
- A valid PFX configuration signs the MSIX; missing or partial signing inputs
  fail with an actionable message and do not log a password.
- Store mode uses the exact provided reserved identity and leaves the package
  unsigned unless signing is explicitly configured.
- Existing `jpackageImage` and `jpackage` tests continue to pass unchanged.

## Follow-up work (not required for the first release)

- Produce an `.appinstaller` feed and support update URI/version configuration.
- Support MSIX bundle (`.msixbundle`) assembly for x64 and arm64 outputs.
- Add richer manifest extensions, file associations, protocol activation, and
  multiple application entries for secondary launchers.
- Run the Windows App Certification Kit as an opt-in CI quality gate.
