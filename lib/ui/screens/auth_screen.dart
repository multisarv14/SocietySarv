// Port of ui/screens/AuthScreen.kt.
//
// Public API:
//   class AuthScreen ({Key? key})                       // login page + registration wizard
//   class LoginFormContent ({Key? key, required VoidCallback onOpenWizard})
//   Future<void> showForgotPinDialog(BuildContext context, {required String prefilledSocietyCode})
//   class ForgotPinDialog ({Key? key, required String prefilledSocietyCode})
//   class SocietyRegistrationWizard ({Key? key, required AppLanguage language, required VoidCallback onDismiss,
//       required void Function(SocietyRegistrationRequest, ValueChanged<String> onError) onFinish})
//   class RegistrationSuccessDialog ({Key? key, required SocietyInfo society, required Resident chairman,
//       required String body, required AppLanguage language, required VoidCallback onContinue})
//   List<UnitTypeConfig> unitTypesOf(List<WingPlan>)
//   List<String> parseWingNames(String raw)
//   Reused by profile_screens.dart:
//     SocietyDetailsStep, LogoUploadRow, PaymentQrUploadRow, UnitDetailsStep, FloorInventoryStep,
//     WingUnitCountsEditor, StepTitle, SubHeading, FieldGap, PinVisibilityToggle,
//     StateTextField (a value/onChanged outlined text field, the Compose OutlinedTextField shape).

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../data/models.dart';
import '../../data/phone_otp_manager.dart';
import '../../l10n/app_strings.dart';
import '../../l10n/location_data.dart';
import '../../state/society_view_model.dart';
import '../../util/format.dart';
import '../../util/image_blob.dart';
import '../components/common_components.dart';
import '../theme/app_colors.dart';

// ------------------------------------------------------------ text field

/// A Compose-style `OutlinedTextField`: the text lives in the parent's state
/// ([value]) and every edit is reported through [onChanged]. A parent that
/// filters what was typed (digits only, max length) simply passes the filtered
/// value back and the field follows it.
class StateTextField extends StatefulWidget {
  const StateTextField({
    super.key,
    required this.value,
    required this.onChanged,
    this.label,
    this.placeholder,
    this.supportingText,
    this.isError = false,
    this.leadingIcon,
    this.trailingIcon,
    this.obscureText = false,
    this.keyboardType,
    this.readOnly = false,
    this.singleLine = true,
    this.minLines,
  });

  final String value;
  final ValueChanged<String> onChanged;
  final String? label;
  final String? placeholder;
  final String? supportingText;
  final bool isError;
  final Widget? leadingIcon;
  final Widget? trailingIcon;
  final bool obscureText;
  final TextInputType? keyboardType;
  final bool readOnly;
  final bool singleLine;
  final int? minLines;

  @override
  State<StateTextField> createState() => _StateTextFieldState();
}

class _StateTextFieldState extends State<StateTextField> {
  late final TextEditingController _controller = TextEditingController(text: widget.value);

  @override
  void didUpdateWidget(covariant StateTextField oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_controller.text != widget.value) {
      _controller.value = TextEditingValue(
        text: widget.value,
        selection: TextSelection.collapsed(offset: widget.value.length),
      );
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final showErrorText = widget.isError && widget.supportingText != null;
    return TextField(
      controller: _controller,
      readOnly: widget.readOnly,
      obscureText: widget.obscureText,
      keyboardType: widget.keyboardType ?? (widget.singleLine ? TextInputType.text : TextInputType.multiline),
      minLines: widget.singleLine ? 1 : (widget.minLines ?? 1),
      maxLines: widget.singleLine ? 1 : null,
      onChanged: (text) {
        if (widget.readOnly) return;
        widget.onChanged(text);
        // When the parent rejects the edit outright its value does not move,
        // so put the text back here.
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted && _controller.text != widget.value) {
            _controller.value = TextEditingValue(
              text: widget.value,
              selection: TextSelection.collapsed(offset: widget.value.length),
            );
          }
        });
      },
      decoration: InputDecoration(
        border: const OutlineInputBorder(),
        labelText: widget.label,
        hintText: widget.placeholder,
        helperText: showErrorText ? null : widget.supportingText,
        helperMaxLines: 3,
        errorText: showErrorText ? widget.supportingText : null,
        errorMaxLines: 3,
        error: widget.isError && !showErrorText ? const SizedBox.shrink() : null,
        prefixIcon: widget.leadingIcon,
        suffixIcon: widget.trailingIcon,
      ),
    );
  }
}

String _digitsOnly(String s, int max) {
  final d = s.replaceAll(RegExp(r'[^0-9]'), '');
  return d.length > max ? d.substring(0, max) : d;
}

const _tealIcon = AppColors.tealPrimary;

// ------------------------------------------------------------- AuthScreen

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  bool _isWizardOpen = false;
  bool _isSuccessDialogOpen = false;

  void _maybeShowSuccess(SocietyViewModel vm) {
    final pending = vm.pendingRegistration;
    if (pending == null || _isSuccessDialogOpen) return;
    _isSuccessDialogOpen = true;
    // Shown once the society exists but before the chairman is signed in, so
    // the login details can actually be handed over (5. Confirmation).
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (dialogContext) => RegistrationSuccessDialog(
          society: pending.society,
          chairman: pending.chairman,
          body: vm.registrationConfirmationText(pending.society, pending.chairman),
          language: vm.language,
          onContinue: () => Navigator.of(dialogContext).pop(),
        ),
      );
      // Dismissing the dialog is the same as Continue.
      _isSuccessDialogOpen = false;
      if (mounted) setState(() => _isWizardOpen = false);
      vm.completeRegistrationSignIn();
    });
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    _maybeShowSuccess(vm);

    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.surface,
      resizeToAvoidBottomInset: true,
      body: SafeArea(
        child: _isWizardOpen
            ? SocietyRegistrationWizard(
                language: language,
                onDismiss: () => setState(() => _isWizardOpen = false),
                onFinish: (request, onError) {
                  vm.registerNewSociety(request, onResult: (success, message) {
                    if (success) {
                      vm.showMessage(message);
                    } else {
                      onError(message);
                    }
                  });
                },
              )
            : LoginFormContent(onOpenWizard: () => setState(() => _isWizardOpen = true)),
      ),
    );
  }
}

// ---------------------------------------------------------------- login page

class LoginFormContent extends StatefulWidget {
  const LoginFormContent({super.key, required this.onOpenWizard});

  final VoidCallback onOpenWizard;

  @override
  State<LoginFormContent> createState() => _LoginFormContentState();
}

class _LoginFormContentState extends State<LoginFormContent> {
  String _societyCode = '';
  String _flatOrMobile = '';
  String _pin = '';
  bool _isPinVisible = false;
  String? _errorMessage;
  // On by default: a member should not have to type their PIN every time the
  // app is opened. Unticking it makes this a one-off, device-shared sign-in.
  bool _rememberMe = true;

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final societyInfo = vm.societyInfo;
    final theme = Theme.of(context);
    final tt = theme.textTheme;
    final cs = theme.colorScheme;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          const SizedBox(height: 16),
          SocietyLogoMark(logoBase64: societyInfo?.logoBase64, size: 96),
          const SizedBox(height: 14),
          Text(
            societyInfo?.name ?? tr('app_title', language),
            style: tt.headlineSmall?.copyWith(fontWeight: FontWeight.bold, color: cs.onSurface),
            textAlign: TextAlign.center,
          ),
          Padding(
            padding: const EdgeInsets.only(top: 4, bottom: 20),
            child: Text(
              tr('app_tagline', language),
              style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
          ),
          Card(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(20),
              side: const BorderSide(color: AppColors.borderLight),
            ),
            color: cs.surface,
            elevation: 2,
            margin: EdgeInsets.zero,
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    tr('login_button', language),
                    style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold, color: cs.onSurface),
                  ),
                  const SizedBox(height: 16),
                  StateTextField(
                    key: const Key('login_society_code_input'),
                    value: _societyCode,
                    onChanged: (v) => setState(() {
                      _societyCode = v;
                      _errorMessage = null;
                    }),
                    label: tr('society_code', language),
                    placeholder: 'e.g. SS002',
                    leadingIcon: const Icon(Icons.shield, color: _tealIcon),
                  ),
                  const SizedBox(height: 12),
                  StateTextField(
                    key: const Key('login_flat_input'),
                    value: _flatOrMobile,
                    onChanged: (v) => setState(() {
                      _flatOrMobile = v;
                      _errorMessage = null;
                    }),
                    label: '${tr('unit_number', language)} / ${tr('mobile_number', language)}',
                    placeholder: 'Unit number or registered mobile',
                    leadingIcon: const Icon(Icons.person, color: _tealIcon),
                  ),
                  const SizedBox(height: 12),
                  StateTextField(
                    key: const Key('login_pin_input'),
                    value: _pin,
                    onChanged: (v) => setState(() {
                      _pin = _digitsOnly(v, 4);
                      _errorMessage = null;
                    }),
                    label: tr('security_pin', language),
                    placeholder: '••••',
                    leadingIcon: const Icon(Icons.lock, color: _tealIcon),
                    trailingIcon: PinVisibilityToggle(
                      isVisible: _isPinVisible,
                      onToggle: () => setState(() => _isPinVisible = !_isPinVisible),
                      testTag: 'login_pin_visibility_btn',
                    ),
                    obscureText: !_isPinVisible,
                    keyboardType: TextInputType.number,
                  ),
                  if (_errorMessage != null) ...[
                    const SizedBox(height: 8),
                    Text(_errorMessage!, style: tt.bodySmall?.copyWith(color: cs.error)),
                  ],
                  const SizedBox(height: 4),
                  // Remember Me: keeps the member signed in the next time the app
                  // is opened. Only the society code and the unit / mobile number
                  // are kept on the device - never the PIN.
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Flexible(
                        child: InkWell(
                          key: const Key('login_remember_me_row'),
                          onTap: () => setState(() => _rememberMe = !_rememberMe),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Checkbox(
                                key: const Key('login_remember_me_checkbox'),
                                value: _rememberMe,
                                activeColor: AppColors.tealPrimary,
                                onChanged: (v) => setState(() => _rememberMe = v ?? false),
                              ),
                              Flexible(child: Text(tr('remember_me', language), style: tt.bodySmall)),
                            ],
                          ),
                        ),
                      ),
                      TextButton(
                        key: const Key('forgot_pin_btn'),
                        onPressed: () => showForgotPinDialog(context, prefilledSocietyCode: _societyCode),
                        child: Text(
                          tr('forgot_pin', language),
                          style: tt.labelLarge?.copyWith(fontWeight: FontWeight.w600, color: AppColors.tealPrimary),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  SizedBox(
                    height: 48,
                    child: FilledButton(
                      key: const Key('login_submit_btn'),
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.tealPrimary,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      onPressed: () {
                        vm.login(_societyCode, _flatOrMobile, _pin, rememberMe: _rememberMe,
                            onResult: (success, msg) {
                          if (!success && mounted) setState(() => _errorMessage = msg);
                        });
                      },
                      child: Text(
                        tr('login_button', language),
                        style: tt.labelLarge?.copyWith(fontWeight: FontWeight.bold, color: Colors.white),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: OutlinedButton.icon(
              key: const Key('register_society_wizard_btn'),
              onPressed: widget.onOpenWizard,
              style: OutlinedButton.styleFrom(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                side: const BorderSide(color: AppColors.tealPrimary, width: 1.5),
              ),
              icon: const Icon(Icons.corporate_fare, color: AppColors.tealPrimary),
              label: Text(
                tr('register_new_society', language),
                style: tt.labelLarge?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
              ),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: Text(
              tr('contact_chairman_note', language),
              key: const Key('contact_chairman_note'),
              style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

class PinVisibilityToggle extends StatelessWidget {
  const PinVisibilityToggle({super.key, required this.isVisible, required this.onToggle, required this.testTag});

  final bool isVisible;
  final VoidCallback onToggle;
  final String testTag;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      key: Key(testTag),
      onPressed: onToggle,
      tooltip: isVisible ? 'Hide PIN' : 'Show PIN',
      icon: Icon(isVisible ? Icons.visibility_off : Icons.visibility, color: AppColors.tealPrimary),
    );
  }
}

// ------------------------------------------------------------- forgot my PIN

Future<void> showForgotPinDialog(BuildContext context, {required String prefilledSocietyCode}) async {
  await showDialog<void>(
    context: context,
    builder: (_) => ForgotPinDialog(prefilledSocietyCode: prefilledSocietyCode),
  );
  PhoneOtpManager.reset();
}

const _stageIdentify = 0;
const _stageVerify = 1;
const _stageSetPin = 2;

/// Resets a login PIN against the mobile number registered for the flat, proven
/// with a Firebase SMS OTP. Runs in three stages: identify -> verify -> set.
class ForgotPinDialog extends StatefulWidget {
  const ForgotPinDialog({super.key, required this.prefilledSocietyCode});

  final String prefilledSocietyCode;

  @override
  State<ForgotPinDialog> createState() => _ForgotPinDialogState();
}

class _ForgotPinDialogState extends State<ForgotPinDialog> {
  int _stage = _stageIdentify;
  late String _societyCode = widget.prefilledSocietyCode;
  String _mobile = '';
  String _otp = '';
  String _newPin = '';
  String _confirmPin = '';
  bool _isPinVisible = false;
  bool _isBusy = false;
  String? _error;
  String? _info;
  Resident? _resolvedMember;
  String _resolvedSocietyCode = '';

  void _update(VoidCallback fn) {
    if (mounted) setState(fn);
  }

  void _close() {
    PhoneOtpManager.reset();
    Navigator.of(context).pop();
  }

  void _onPrimary(SocietyViewModel vm) {
    setState(() {
      _error = null;
      _info = null;
    });
    switch (_stage) {
      case _stageIdentify:
        setState(() => _isBusy = true);
        vm.findMemberForPinReset(_societyCode, _mobile, onResult: (member, detail) {
          if (member == null) {
            _update(() {
              _isBusy = false;
              _error = detail;
            });
          } else {
            _update(() {
              _resolvedMember = member;
              _resolvedSocietyCode = detail;
            });
            PhoneOtpManager.sendOtp(
              phoneNumber: _mobile,
              onCodeSent: () => _update(() {
                _isBusy = false;
                _stage = _stageVerify;
              }),
              onAutoVerified: () => _update(() {
                _isBusy = false;
                _stage = _stageSetPin;
              }),
              onError: (e) => _update(() {
                _isBusy = false;
                _error = e;
              }),
            );
          }
        });
      case _stageVerify:
        if (_otp.length != 6) {
          setState(() => _error = 'Enter the 6-digit OTP.');
          return;
        }
        setState(() => _isBusy = true);
        PhoneOtpManager.verifyCode(_otp, (ok, detail) {
          _update(() {
            _isBusy = false;
            if (ok) {
              _stage = _stageSetPin;
            } else {
              _error = detail;
            }
          });
        });
      default:
        if (_newPin.length != 4) {
          setState(() => _error = 'The PIN must be exactly 4 digits.');
          return;
        }
        if (_newPin != _confirmPin) {
          setState(() => _error = 'The two PINs do not match.');
          return;
        }
        final member = _resolvedMember;
        if (member == null) return;
        setState(() => _isBusy = true);
        vm.resetPinAfterOtp(_resolvedSocietyCode, member, _newPin, onResult: (ok, detail) {
          if (!mounted) return;
          setState(() => _isBusy = false);
          if (ok) {
            vm.showMessage(detail);
            _close();
          } else {
            setState(() => _error = detail);
          }
        });
    }
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<SocietyViewModel>();
    final language = vm.language;
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final flat = _resolvedMember?.flatNumber ?? '';

    final List<Widget> stageContent;
    switch (_stage) {
      case _stageIdentify:
        stageContent = [
          Text(
            switch (language) {
              AppLanguage.marathi =>
                'तुमचा सोसायटी लॉगिन कोड व नोंदणीकृत मोबाईल क्रमांक टाका. त्या क्रमांकावर OTP पाठवला जाईल.',
              AppLanguage.hindi => 'अपना सोसायटी लॉगिन कोड और पंजीकृत मोबाइल नंबर डालें। उस नंबर पर ओटीपी भेजा जाएगा।',
              AppLanguage.english =>
                'Enter your society login code and the mobile number registered against your flat. We will send an OTP to that number.',
            },
            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
          ),
          const SizedBox(height: 12),
          StateTextField(
            key: const Key('forgot_society_code_input'),
            value: _societyCode,
            onChanged: (v) => setState(() {
              _societyCode = v;
              _error = null;
            }),
            label: tr('society_code', language),
          ),
          const SizedBox(height: 10),
          StateTextField(
            key: const Key('forgot_mobile_input'),
            value: _mobile,
            onChanged: (v) => setState(() {
              _mobile = _digitsOnly(v, 10);
              _error = null;
            }),
            label: tr('mobile_number', language),
            leadingIcon: const Icon(Icons.phone, color: _tealIcon),
            keyboardType: TextInputType.phone,
          ),
        ];
      case _stageVerify:
        stageContent = [
          Text(
            'OTP sent to ${PhoneOtpManager.toE164(_mobile)} ($flat).',
            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
          ),
          const SizedBox(height: 12),
          StateTextField(
            key: const Key('forgot_otp_input'),
            value: _otp,
            onChanged: (v) => setState(() {
              _otp = _digitsOnly(v, 6);
              _error = null;
            }),
            label: tr('enter_otp', language),
            keyboardType: TextInputType.number,
          ),
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(
              onPressed: _isBusy
                  ? null
                  : () {
                      setState(() {
                        _isBusy = true;
                        _error = null;
                      });
                      PhoneOtpManager.sendOtp(
                        phoneNumber: _mobile,
                        resend: true,
                        onCodeSent: () => _update(() {
                          _isBusy = false;
                          _info = 'OTP sent again.';
                        }),
                        onAutoVerified: () => _update(() {
                          _isBusy = false;
                          _stage = _stageSetPin;
                        }),
                        onError: (e) => _update(() {
                          _isBusy = false;
                          _error = e;
                        }),
                      );
                    },
              child: Text(tr('resend_otp', language), style: const TextStyle(color: AppColors.tealPrimary)),
            ),
          ),
        ];
      default:
        stageContent = [
          Text(
            'Verified. Set a new 4-digit login PIN for $flat.',
            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
          ),
          const SizedBox(height: 12),
          StateTextField(
            key: const Key('forgot_new_pin_input'),
            value: _newPin,
            onChanged: (v) => setState(() {
              _newPin = _digitsOnly(v, 4);
              _error = null;
            }),
            label: tr('new_pin', language),
            leadingIcon: const Icon(Icons.lock, color: _tealIcon),
            trailingIcon: PinVisibilityToggle(
              isVisible: _isPinVisible,
              onToggle: () => setState(() => _isPinVisible = !_isPinVisible),
              testTag: 'forgot_pin_visibility_btn',
            ),
            obscureText: !_isPinVisible,
            keyboardType: TextInputType.number,
          ),
          const SizedBox(height: 10),
          StateTextField(
            key: const Key('forgot_confirm_pin_input'),
            value: _confirmPin,
            onChanged: (v) => setState(() {
              _confirmPin = _digitsOnly(v, 4);
              _error = null;
            }),
            label: tr('confirm_new_pin', language),
            leadingIcon: const Icon(Icons.lock, color: _tealIcon),
            obscureText: !_isPinVisible,
            keyboardType: TextInputType.number,
            isError: _confirmPin.isNotEmpty && _confirmPin != _newPin,
          ),
        ];
    }

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      title: Text(
        tr('reset_pin_title', language),
        style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold),
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              ...stageContent,
              if (_info != null) ...[
                const SizedBox(height: 8),
                Text(_info!, style: tt.bodySmall?.copyWith(color: AppColors.tealPrimary)),
              ],
              if (_error != null) ...[
                const SizedBox(height: 8),
                Text(
                  _error!,
                  key: const Key('forgot_pin_error'),
                  style: tt.bodySmall?.copyWith(color: cs.error),
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        OutlinedButton(onPressed: _close, child: Text(tr('cancel', language))),
        FilledButton(
          key: const Key('forgot_pin_primary_btn'),
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: _isBusy ? null : () => _onPrimary(vm),
          child: Text(switch (_stage) {
            _stageIdentify => tr('send_otp', language),
            _stageVerify => tr('verify_otp', language),
            _ => tr('save_new_pin', language),
          }),
        ),
      ],
    );
  }
}

// ------------------------------------------------------- registration wizard

const _totalSteps = 5;

/// 5-step society registration.
///
/// 1. Society Details   - identity, address, contact, logo, bank account
/// 2. Unit Details      - how many wings and what they are called
/// 3. Floor Inventory   - floors and units per wing, plus the unit types
/// 4. Office Bearers    - the chairman and the 4-digit PIN he logs in with
/// 5. Confirmation      - review, then hand the login details over
class SocietyRegistrationWizard extends StatefulWidget {
  const SocietyRegistrationWizard({
    super.key,
    required this.language,
    required this.onDismiss,
    required this.onFinish,
  });

  final AppLanguage language;
  final VoidCallback onDismiss;
  final void Function(SocietyRegistrationRequest request, ValueChanged<String> onError) onFinish;

  @override
  State<SocietyRegistrationWizard> createState() => _SocietyRegistrationWizardState();
}

class _SocietyRegistrationWizardState extends State<SocietyRegistrationWizard> {
  int _step = 1;
  String? _errorMessage;

  // 1. Society Details
  String _societyName = '';
  String _regNumber = '';
  String _societyCode = '';
  String _country = LocationData.defaultCountry;
  String _state = '';
  String _city = '';
  String _address = '';
  String _pinCode = '';
  String _societyPhone = '';
  String _societyEmail = '';
  String _logoBase64 = '';
  String _bankName = '';
  String _bankAccount = '';
  String _ifscCode = '';
  String _upiId = '';
  String _paymentQrBase64 = '';
  String _initialBankBalance = '';
  // Billing opens on the current month unless the committee moves it back.
  String _billingStartMonth = currentMonthLabel();

  // 2. Unit Details
  String _totalWings = '';
  String _wingNamesRaw = '';

  // 3. Floor Inventory
  List<WingPlan> _wingPlans = const [];

  // 4. Office Bearers
  String _chairmanName = '';
  String _chairmanPhone = '';
  String _chairmanEmail = '';
  String _chairmanFlat = '';
  String _chairmanPin = '';
  String _confirmPin = '';

  void _set(VoidCallback fn) => setState(fn);

  String? _stepError(List<String> wingNames) {
    final wingCount = int.tryParse(_totalWings) ?? 0;
    switch (_step) {
      case 1:
        if (_societyName.trim().isEmpty) return 'Enter the society name.';
        if (_societyCode.trim().isEmpty) return 'Enter a society login code - members sign in with it.';
        if (_pinCode.trim().isNotEmpty && _pinCode.length != 6) return 'PIN code must be exactly 6 digits.';
        if (_societyEmail.trim().isNotEmpty && !_societyEmail.contains('@')) return 'Enter a valid society email id.';
        return null;
      case 2:
        if (wingCount <= 0) return 'Enter how many wings the society has.';
        if (wingNames.isEmpty) return 'Enter the wing names, separated by commas.';
        if (wingNames.length != wingCount) {
          return 'You entered ${wingNames.length} wing name(s) for $wingCount wing(s).';
        }
        return null;
      case 3:
        if (!_wingPlans.any((p) => p.totalUnits > 0)) {
          return 'Add at least one unit type with a quantity, and save the wing.';
        }
        return null;
      case 4:
        if (_chairmanName.trim().isEmpty) return "Enter the chairman's full name.";
        if (_chairmanFlat.trim().isEmpty) return "Enter the chairman's unit number.";
        if (_chairmanPhone.length < 10) return 'Enter a valid 10-digit mobile number.';
        if (_chairmanPin.length != 4) return 'The login PIN must be exactly 4 digits.';
        if (_chairmanPin != _confirmPin) return 'The two PINs do not match.';
        return null;
      default:
        return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final language = widget.language;
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final wingNames = parseWingNames(_wingNamesRaw);
    final plannedUnits = _wingPlans.fold<int>(0, (s, p) => s + p.totalUnits);
    // The society's unit types are simply the ones its wings were built from;
    // what each type is charged per month is set under Maintenance > Tariff.
    final unitTypes = unitTypesOf(_wingPlans);
    final stepError = _stepError(wingNames);

    final Widget stepBody;
    switch (_step) {
      case 1:
        stepBody = SocietyDetailsStep(
          language: language,
          societyName: _societyName,
          onSocietyName: (v) => _set(() => _societyName = v),
          regNumber: _regNumber,
          onRegNumber: (v) => _set(() => _regNumber = v),
          societyCode: _societyCode,
          onSocietyCode: (v) => _set(() {
            _societyCode = v;
            _errorMessage = null;
          }),
          country: _country,
          onCountry: (v) => _set(() {
            _country = v;
            _state = '';
            _city = '';
          }),
          state: _state,
          onState: (v) => _set(() {
            _state = v;
            _city = '';
          }),
          city: _city,
          onCity: (v) => _set(() => _city = v),
          address: _address,
          onAddress: (v) => _set(() => _address = v),
          pinCode: _pinCode,
          onPinCode: (v) => _set(() => _pinCode = v),
          phone: _societyPhone,
          onPhone: (v) => _set(() => _societyPhone = v),
          email: _societyEmail,
          onEmail: (v) => _set(() => _societyEmail = v),
          logoBase64: _logoBase64,
          onLogo: (v) => _set(() => _logoBase64 = v),
          bankName: _bankName,
          onBankName: (v) => _set(() => _bankName = v),
          bankAccount: _bankAccount,
          onBankAccount: (v) => _set(() => _bankAccount = v),
          ifscCode: _ifscCode,
          onIfsc: (v) => _set(() => _ifscCode = v),
          upiId: _upiId,
          onUpiId: (v) => _set(() => _upiId = v),
          paymentQrBase64: _paymentQrBase64,
          onPaymentQr: (v) => _set(() => _paymentQrBase64 = v),
          initialBankBalance: _initialBankBalance,
          onInitialBalance: (v) => _set(() => _initialBankBalance = v),
          billingStartMonth: _billingStartMonth,
          onBillingStartMonth: (v) => _set(() => _billingStartMonth = v),
        );
      case 2:
        stepBody = UnitDetailsStep(
          language: language,
          totalWings: _totalWings,
          onTotalWings: (v) => _set(() => _totalWings = v),
          wingNamesRaw: _wingNamesRaw,
          onWingNamesRaw: (v) => _set(() => _wingNamesRaw = v),
          parsedWingNames: wingNames,
        );
      case 3:
        stepBody = FloorInventoryStep(
          language: language,
          wingNames: wingNames,
          wingPlans: _wingPlans,
          onWingPlans: (v) => _set(() => _wingPlans = v),
        );
      case 4:
        stepBody = _OfficeBearersStep(
          language: language,
          name: _chairmanName,
          onName: (v) => _set(() => _chairmanName = v),
          flat: _chairmanFlat,
          onFlat: (v) => _set(() => _chairmanFlat = v),
          phone: _chairmanPhone,
          onPhone: (v) => _set(() => _chairmanPhone = v),
          email: _chairmanEmail,
          onEmail: (v) => _set(() => _chairmanEmail = v),
          pin: _chairmanPin,
          onPin: (v) => _set(() => _chairmanPin = v),
          confirmPin: _confirmPin,
          onConfirmPin: (v) => _set(() => _confirmPin = v),
        );
      default:
        stepBody = _ConfirmationStep(
          language: language,
          societyName: _societyName,
          societyCode: _societyCode,
          regNumber: _regNumber,
          location: [_city, _state, _country].where((s) => s.trim().isNotEmpty).join(', '),
          address: _address,
          pinCode: _pinCode,
          societyPhone: _societyPhone,
          societyEmail: _societyEmail,
          logoBase64: _logoBase64,
          wingNames: wingNames,
          wingPlans: _wingPlans,
          unitTypes: unitTypes,
          plannedUnits: plannedUnits,
          chairmanName: _chairmanName,
          chairmanFlat: _chairmanFlat,
          chairmanPhone: _chairmanPhone,
          chairmanEmail: _chairmanEmail,
          bankName: _bankName,
          initialBankBalance: _initialBankBalance,
        );
    }

    final blocker = _errorMessage ?? stepError;

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              IconButton(
                key: const Key('wizard_back_btn'),
                tooltip: 'Back',
                onPressed: () {
                  if (_step > 1) {
                    setState(() => _step--);
                  } else {
                    widget.onDismiss();
                  }
                },
                icon: const Icon(Icons.arrow_back),
              ),
              Expanded(
                child: Text(
                  tr('register_new_society', language),
                  style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
              ),
              Text(
                '$_step / $_totalSteps',
                style: tt.labelMedium?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
              ),
            ],
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: LinearProgressIndicator(value: _step / _totalSteps, color: AppColors.tealPrimary),
          ),
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: KeyedSubtree(key: ValueKey('wizard_step_$_step'), child: stepBody),
            ),
          ),
          if (blocker != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Text(
                blocker,
                key: const Key('wizard_error_text'),
                style: tt.bodySmall?.copyWith(color: cs.error),
              ),
            ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                OutlinedButton(
                  key: const Key('wizard_cancel_btn'),
                  onPressed: widget.onDismiss,
                  child: Text(tr('cancel', language)),
                ),
                FilledButton(
                  key: const Key('wizard_next_btn'),
                  style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
                  onPressed: stepError != null
                      ? null
                      : () {
                          setState(() => _errorMessage = null);
                          if (_step < _totalSteps) {
                            setState(() => _step++);
                          } else {
                            widget.onFinish(
                              SocietyRegistrationRequest(
                                name: _societyName,
                                regNumber: _regNumber,
                                code: _societyCode,
                                country: _country,
                                state: _state,
                                city: _city,
                                address: _address,
                                pinCode: _pinCode,
                                contactNumber: _societyPhone,
                                email: _societyEmail,
                                logoBase64: _logoBase64,
                                bankName: _bankName,
                                bankAccount: _bankAccount,
                                ifsc: _ifscCode,
                                upiId: _upiId,
                                paymentQrBase64: _paymentQrBase64,
                                initialBankBalance: double.tryParse(_initialBankBalance) ?? 0.0,
                                billingStartMonth: _billingStartMonth,
                                wingNames: wingNames,
                                wingPlans: _wingPlans,
                                unitTypes: unitTypes.where((t) => t.name.trim().isNotEmpty).toList(),
                                chairmanName: _chairmanName,
                                chairmanPhone: _chairmanPhone,
                                chairmanEmail: _chairmanEmail,
                                chairmanFlat: _chairmanFlat,
                                chairmanPin: _chairmanPin,
                              ),
                              (message) {
                                if (mounted) setState(() => _errorMessage = message);
                              },
                            );
                          }
                        },
                  child: Text(
                    _step < _totalSteps ? tr('save_continue', language) : tr('finish_setup', language),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// The distinct unit types a set of wings is built from, in the order they were
/// first entered. Tariffs start at zero and are set under Maintenance > Tariff.
List<UnitTypeConfig> unitTypesOf(List<WingPlan> wingPlans) {
  final seen = <String>{};
  final result = <UnitTypeConfig>[];
  for (final plan in wingPlans) {
    for (final count in plan.unitCounts) {
      final name = count.unitType.trim();
      if (name.isEmpty) continue;
      if (seen.add(name.toLowerCase())) result.add(UnitTypeConfig(name: name, monthlyTariff: 0.0));
    }
  }
  return result;
}

/// "A, B , c" -> ["A", "B", "C"], duplicates and stray commas dropped.
List<String> parseWingNames(String raw) {
  final result = <String>[];
  for (final part in raw.split(',')) {
    final name = part.trim().toUpperCase();
    if (name.isNotEmpty && !result.contains(name)) result.add(name);
  }
  return result;
}

// ------------------------------------------------------ step 1: society details

class SocietyDetailsStep extends StatelessWidget {
  const SocietyDetailsStep({
    super.key,
    required this.language,
    required this.societyName,
    required this.onSocietyName,
    required this.regNumber,
    required this.onRegNumber,
    required this.societyCode,
    required this.onSocietyCode,
    required this.country,
    required this.onCountry,
    required this.state,
    required this.onState,
    required this.city,
    required this.onCity,
    required this.address,
    required this.onAddress,
    required this.pinCode,
    required this.onPinCode,
    required this.phone,
    required this.onPhone,
    required this.email,
    required this.onEmail,
    required this.logoBase64,
    required this.onLogo,
    required this.bankName,
    required this.onBankName,
    required this.bankAccount,
    required this.onBankAccount,
    required this.ifscCode,
    required this.onIfsc,
    required this.upiId,
    required this.onUpiId,
    required this.paymentQrBase64,
    required this.onPaymentQr,
    required this.initialBankBalance,
    required this.onInitialBalance,
    required this.billingStartMonth,
    required this.onBillingStartMonth,
    this.codeEditable = true,
    this.showOpeningBalance = true,
  });

  final AppLanguage language;
  final String societyName;
  final ValueChanged<String> onSocietyName;
  final String regNumber;
  final ValueChanged<String> onRegNumber;
  final String societyCode;
  final ValueChanged<String> onSocietyCode;
  final String country;
  final ValueChanged<String> onCountry;
  final String state;
  final ValueChanged<String> onState;
  final String city;
  final ValueChanged<String> onCity;
  final String address;
  final ValueChanged<String> onAddress;
  final String pinCode;
  final ValueChanged<String> onPinCode;
  final String phone;
  final ValueChanged<String> onPhone;
  final String email;
  final ValueChanged<String> onEmail;
  final String logoBase64;
  final ValueChanged<String> onLogo;
  final String bankName;
  final ValueChanged<String> onBankName;
  final String bankAccount;
  final ValueChanged<String> onBankAccount;
  final String ifscCode;
  final ValueChanged<String> onIfsc;
  final String upiId;
  final ValueChanged<String> onUpiId;
  final String paymentQrBase64;
  final ValueChanged<String> onPaymentQr;
  final String initialBankBalance;
  final ValueChanged<String> onInitialBalance;
  final String billingStartMonth;
  final ValueChanged<String> onBillingStartMonth;

  /// False once the society exists: the login code is what every member signs
  /// in against, so the Society Profile shows it without letting it be edited.
  final bool codeEditable;

  /// False in the Society Profile - the opening balance belongs to the ledger.
  final bool showOpeningBalance;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        StepTitle(tr('wizard_step1', language)),
        StateTextField(
          key: const Key('wizard_society_name_input'),
          value: societyName,
          onChanged: onSocietyName,
          label: 'Society Name (संस्थेचे नाव)',
        ),
        const FieldGap(),
        StateTextField(value: regNumber, onChanged: onRegNumber, label: 'Registration No. (नोंदणी क्रमांक)'),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_society_code_input'),
          value: societyCode,
          onChanged: (v) {
            if (codeEditable) onSocietyCode(v);
          },
          readOnly: !codeEditable,
          label: 'Society Login Code (लॉगिन कोड)',
          placeholder: 'e.g. SS002',
          supportingText: codeEditable ? null : 'Members sign in with this code, so it cannot be changed.',
        ),
        const FieldGap(),
        SearchableDropdownField(
          label: 'Country (देश)',
          value: country,
          options: LocationData.countries,
          onValueChange: onCountry,
          testTag: 'wizard_country_dropdown',
        ),
        const FieldGap(),
        SearchableDropdownField(
          label: 'State (राज्य)',
          value: state,
          options: LocationData.statesOf(country),
          onValueChange: onState,
          testTag: 'wizard_state_dropdown',
        ),
        const FieldGap(),
        SearchableDropdownField(
          label: 'City (शहर)',
          value: city,
          options: LocationData.citiesOf(country, state),
          onValueChange: onCity,
          testTag: 'wizard_city_dropdown',
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_address_input'),
          value: address,
          onChanged: onAddress,
          label: 'Society Address (पत्ता)',
          singleLine: false,
          minLines: 2,
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_pincode_input'),
          value: pinCode,
          onChanged: (v) => onPinCode(_digitsOnly(v, 6)),
          label: 'PIN Code (पिन कोड)',
          placeholder: '6 digits',
          supportingText: '${pinCode.length}/6',
          isError: pinCode.trim().isNotEmpty && pinCode.length != 6,
          keyboardType: TextInputType.number,
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_society_phone_input'),
          value: phone,
          onChanged: (v) => onPhone(_digitsOnly(v, 10)),
          label: 'Society Contact Number (संपर्क क्रमांक)',
          leadingIcon: const Icon(Icons.phone, color: _tealIcon),
          keyboardType: TextInputType.phone,
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_society_email_input'),
          value: email,
          onChanged: onEmail,
          label: 'Society Email ID (ईमेल)',
          leadingIcon: const Icon(Icons.email, color: _tealIcon),
          keyboardType: TextInputType.emailAddress,
        ),
        const FieldGap(),
        LogoUploadRow(logoBase64: logoBase64, onLogo: onLogo),
        const SizedBox(height: 18),
        const SubHeading('Society Bank Account'),
        const SizedBox(height: 8),
        StateTextField(
          value: bankName,
          onChanged: onBankName,
          label: 'Society Bank Name (बँकेचे नाव)',
          leadingIcon: const Icon(Icons.account_balance, color: _tealIcon),
        ),
        const FieldGap(),
        StateTextField(
          value: bankAccount,
          onChanged: onBankAccount,
          label: 'Bank Account Number (खाते क्र.)',
          keyboardType: TextInputType.number,
        ),
        const FieldGap(),
        // Every payment QR in the app is built from this.
        StateTextField(
          key: const Key('wizard_upi_id_input'),
          value: upiId,
          onChanged: onUpiId,
          label: 'UPI ID (यूपीआय आयडी)',
          placeholder: 'society@okhdfcbank',
          supportingText: "Members' payment QR codes are generated from this UPI ID.",
        ),
        const FieldGap(),
        PaymentQrUploadRow(qrBase64: paymentQrBase64, onQr: onPaymentQr),
        const FieldGap(),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(child: StateTextField(value: ifscCode, onChanged: onIfsc, label: 'IFSC Code')),
            if (showOpeningBalance) ...[
              const SizedBox(width: 8),
              Expanded(
                child: StateTextField(
                  value: initialBankBalance,
                  onChanged: onInitialBalance,
                  label: 'Initial Balance (₹)',
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                ),
              ),
            ],
          ],
        ),
        const SizedBox(height: 18),
        const SubHeading('Billing Start Month & Year'),
        const SizedBox(height: 8),
        MonthYearPickerField(
          label: 'Society Bill Starting Month & Year',
          value: billingStartMonth,
          onValueChange: onBillingStartMonth,
          supportingText: 'Billing runs from this month onwards - every bill month '
              'list in the app starts here.',
          testTagPrefix: 'wizard_billing_start',
        ),
      ],
    );
  }
}

/// Square 64dp preview tile used by the logo and payment-QR upload rows.
class _ImagePreviewTile extends StatelessWidget {
  const _ImagePreviewTile({required this.base64, required this.fallback});

  final String base64;
  final IconData fallback;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(14),
      child: Container(
        width: 64,
        height: 64,
        color: AppColors.surfaceVariant,
        alignment: Alignment.center,
        child: BlobImage(
          base64: base64,
          width: 64,
          height: 64,
          fit: BoxFit.cover,
          placeholder: Icon(fallback, color: AppColors.tealPrimary),
        ),
      ),
    );
  }
}

/// Picks an image, shrinks it and keeps it as the Base64 blob on the society doc.
class LogoUploadRow extends StatefulWidget {
  const LogoUploadRow({super.key, required this.logoBase64, required this.onLogo});

  final String logoBase64;
  final ValueChanged<String> onLogo;

  @override
  State<LogoUploadRow> createState() => _LogoUploadRowState();
}

class _LogoUploadRowState extends State<LogoUploadRow> {
  String? _pickError;

  Future<void> _pick() async {
    try {
      final encoded = await pickImageAsBase64(context, source: ImageSource.gallery);
      if (encoded == null || !mounted) return;
      widget.onLogo(encoded);
      setState(() => _pickError = null);
    } catch (_) {
      if (mounted) setState(() => _pickError = 'Could not read that image. Pick a JPG or PNG.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final hasLogo = widget.logoBase64.trim().isNotEmpty;
    return Row(
      children: [
        _ImagePreviewTile(base64: widget.logoBase64, fallback: Icons.add_photo_alternate),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              OutlinedButton(
                key: const Key('wizard_logo_upload_btn'),
                onPressed: _pick,
                style: OutlinedButton.styleFrom(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  side: const BorderSide(color: AppColors.tealPrimary),
                ),
                child: Text(
                  hasLogo ? 'Change Logo' : 'Upload Society Logo',
                  style: const TextStyle(color: AppColors.tealPrimary),
                ),
              ),
              if (hasLogo)
                TextButton(
                  onPressed: () => widget.onLogo(''),
                  child: Text('Remove', style: TextStyle(color: cs.error)),
                ),
              if (_pickError != null) Text(_pickError!, style: tt.bodySmall?.copyWith(color: cs.error)),
            ],
          ),
        ),
      ],
    );
  }
}

/// The society's own payment QR, for banks whose code carries more than the VPA.
///
/// A generated QR is built from the UPI id alone. Some providers issue a QR that
/// also carries a merchant id or a signature, and only that exact image will
/// resolve - so the chairman can upload theirs, and members are shown it in
/// place of the generated one.
class PaymentQrUploadRow extends StatefulWidget {
  const PaymentQrUploadRow({super.key, required this.qrBase64, required this.onQr});

  final String qrBase64;
  final ValueChanged<String> onQr;

  @override
  State<PaymentQrUploadRow> createState() => _PaymentQrUploadRowState();
}

class _PaymentQrUploadRowState extends State<PaymentQrUploadRow> {
  String? _pickError;

  Future<void> _pick() async {
    try {
      final picked = await ImagePicker().pickImage(source: ImageSource.gallery);
      if (picked == null) return;
      final bytes = await picked.readAsBytes();
      // A QR is read module by module, so it is kept at a higher quality than
      // a logo - JPEG smudging the modules is the difference between a code
      // that scans and one that does not.
      final encoded = await ImageBlob.fromBytesCompressed(bytes, quality: 95);
      if (!mounted) return;
      widget.onQr(encoded);
      setState(() => _pickError = null);
    } catch (_) {
      if (mounted) setState(() => _pickError = 'Could not read that image. Pick a JPG or PNG of the QR.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final hasQr = widget.qrBase64.trim().isNotEmpty;
    return Row(
      children: [
        _ImagePreviewTile(base64: widget.qrBase64, fallback: Icons.qr_code_2),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              OutlinedButton(
                key: const Key('wizard_payment_qr_upload_btn'),
                onPressed: _pick,
                style: OutlinedButton.styleFrom(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  side: const BorderSide(color: AppColors.tealPrimary),
                ),
                child: Text(
                  hasQr ? 'Change Payment QR' : 'Upload Payment QR',
                  style: const TextStyle(color: AppColors.tealPrimary),
                ),
              ),
              Text(
                hasQr
                    ? 'Members will be shown this QR instead of the generated one.'
                    : 'Optional. Leave this empty and members get a QR generated from the UPI ID above.',
                style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
              ),
              if (hasQr)
                TextButton(
                  key: const Key('wizard_payment_qr_remove_btn'),
                  onPressed: () => widget.onQr(''),
                  child: Text('Remove', style: TextStyle(color: cs.error)),
                ),
              if (_pickError != null) Text(_pickError!, style: tt.bodySmall?.copyWith(color: cs.error)),
            ],
          ),
        ),
      ],
    );
  }
}

// --------------------------------------------------------- step 2: unit details

class UnitDetailsStep extends StatelessWidget {
  const UnitDetailsStep({
    super.key,
    required this.language,
    required this.totalWings,
    required this.onTotalWings,
    required this.wingNamesRaw,
    required this.onWingNamesRaw,
    required this.parsedWingNames,
    this.strayWings = const [],
    this.unitCountFor,
    this.onRenameUnits,
    this.onDeleteStrayWing,
    this.onDeleteWingWithUnits,
  });

  final AppLanguage language;
  final String totalWings;
  final ValueChanged<String> onTotalWings;
  final String wingNamesRaw;
  final ValueChanged<String> onWingNamesRaw;
  final List<String> parsedWingNames;

  /// Wings that exist on the society's units but are not on the list above -
  /// typed in before the wing list was set up. Shown so they can be seen and
  /// tidied rather than staying invisible.
  final List<String> strayWings;

  /// How many units carry a given wing name.
  final int Function(String wing)? unitCountFor;

  /// Renames a wing on the units themselves, not just on this form.
  final void Function(String from, String to)? onRenameUnits;

  /// Forgets a stray wing that has no units left on it.
  final ValueChanged<String>? onDeleteStrayWing;

  /// Deletes a wing along with every unit on it.
  final ValueChanged<String>? onDeleteWingWithUnits;

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final count = int.tryParse(totalWings) ?? 0;
    int unitsOf(String wing) => unitCountFor?.call(wing) ?? 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        StepTitle(tr('wizard_step2', language)),
        StateTextField(
          key: const Key('wizard_total_wings_input'),
          value: totalWings,
          onChanged: (v) => onTotalWings(_digitsOnly(v, 2)),
          label: 'Total Wings (एकूण विंग)',
          keyboardType: TextInputType.number,
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_wing_names_input'),
          value: wingNamesRaw,
          onChanged: onWingNamesRaw,
          label: 'Enter Wing Names (विंगची नावे)',
          placeholder: 'A, B, C',
          supportingText: parsedWingNames.isEmpty
              ? 'Separate each wing name with a comma.'
              : '${parsedWingNames.length} wing(s): ${parsedWingNames.join(', ')}',
        ),
        if (count >= 1 && count <= 26) ...[
          const FieldGap(),
          Align(
            alignment: Alignment.centerLeft,
            child: OutlinedButton(
              key: const Key('wizard_autofill_wings_btn'),
              onPressed: () =>
                  onWingNamesRaw([for (var i = 0; i < count; i++) String.fromCharCode(65 + i)].join(', ')),
              style: OutlinedButton.styleFrom(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                side: const BorderSide(color: AppColors.tealPrimary),
              ),
              child: Text(
                'Auto-fill A … ${String.fromCharCode(65 + count - 1)}',
                style: const TextStyle(color: AppColors.tealPrimary),
              ),
            ),
          ),
        ],
        // The wings as they stand, so a name can be corrected or a wing dropped
        // without retyping the whole comma-separated line.
        if (parsedWingNames.isNotEmpty || strayWings.isNotEmpty) ...[
          const SizedBox(height: 16),
          const SubHeading('Wings Added'),
          const SizedBox(height: 6),
          for (final wing in parsedWingNames)
            _WingRow(
              key: ValueKey('wing_row_state_$wing'),
              wing: wing,
              unitCount: unitsOf(wing),
              isStray: false,
              onRename: (renamed) {
                final updated = [for (final w in parsedWingNames) w == wing ? renamed : w];
                onWingNamesRaw(updated.join(', '));
                onTotalWings(updated.length.toString());
                onRenameUnits?.call(wing, renamed);
              },
              onDelete: () {
                if (unitsOf(wing) == 0) {
                  final updated = parsedWingNames.where((w) => w != wing).toList();
                  onWingNamesRaw(updated.join(', '));
                  onTotalWings(updated.length.toString());
                } else {
                  // The wing holds flats, so removing it has to take them
                  // with it - leaving them on a wing that no longer exists is
                  // what made them unreachable in the first place.
                  onDeleteWingWithUnits?.call(wing);
                }
              },
            ),
          if (strayWings.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(
              "These wings are on the society's units but not on the list "
              'above. Rename one onto a wing you keep, and its units move with it.',
              style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
            ),
            const SizedBox(height: 6),
            for (final wing in strayWings)
              _WingRow(
                key: ValueKey('stray_wing_row_state_$wing'),
                wing: wing,
                unitCount: unitsOf(wing),
                isStray: true,
                onRename: (renamed) => onRenameUnits?.call(wing, renamed),
                // A stray wing with nothing on it is just a leftover name - it
                // can go. One with units has to be renamed instead, so its
                // flats travel with it.
                onDelete: () {
                  if (unitsOf(wing) == 0) {
                    onDeleteStrayWing?.call(wing);
                  } else {
                    onDeleteWingWithUnits?.call(wing);
                  }
                },
              ),
          ],
        ],
      ],
    );
  }
}

/// One wing in the Unit Details list.
///
/// Renaming a wing renames it on the units that carry it too, so the society's
/// list and its flats never drift apart.
class _WingRow extends StatefulWidget {
  const _WingRow({
    super.key,
    required this.wing,
    required this.unitCount,
    required this.isStray,
    required this.onRename,
    required this.onDelete,
  });

  final String wing;
  final int unitCount;
  final bool isStray;
  final ValueChanged<String> onRename;
  final VoidCallback? onDelete;

  @override
  State<_WingRow> createState() => _WingRowState();
}

class _WingRowState extends State<_WingRow> {
  bool _isEditing = false;
  late String _draft = widget.wing;

  Future<void> _confirmDelete() async {
    final wing = widget.wing;
    final unitCount = widget.unitCount;
    final onDelete = widget.onDelete;
    if (onDelete == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        final tt = Theme.of(dialogContext).textTheme;
        final cs = Theme.of(dialogContext).colorScheme;
        return AlertDialog(
          insetPadding: dialogInsetPadding(dialogContext),
          title: Text('Remove Wing $wing?', style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
          content: Text(
            unitCount > 0
                ? 'Wing $wing and all $unitCount unit(s) on it will be deleted, along '
                    'with any bills raised against them. This cannot be undone.\n\n'
                    'To keep the flats, rename the wing instead - they move with it.'
                : "Wing $wing is dropped from the society's wing list. No units carry it.",
            style: tt.bodySmall,
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Cancel')),
            FilledButton(
              key: Key('confirm_delete_wing_$wing'),
              style: FilledButton.styleFrom(backgroundColor: cs.error),
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: Text(unitCount > 0 ? 'Delete Wing & Units' : 'Remove'),
            ),
          ],
        );
      },
    );
    if (confirmed == true) onDelete();
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final wing = widget.wing;
    return Card(
      key: Key('wing_row_$wing'),
      color: AppColors.surfaceVariant,
      elevation: 0,
      margin: const EdgeInsets.only(bottom: 6),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        child: Row(
          children: _isEditing
              ? [
                  Expanded(
                    child: StateTextField(
                      key: Key('wing_edit_input_$wing'),
                      value: _draft,
                      onChanged: (v) => setState(() {
                        final up = v.toUpperCase();
                        _draft = up.length > 24 ? up.substring(0, 24) : up;
                      }),
                      label: 'Wing Name',
                      supportingText: widget.unitCount > 0 ? 'Renames ${widget.unitCount} unit(s) too' : null,
                    ),
                  ),
                  TextButton(
                    onPressed: _draft.trim().isEmpty
                        ? null
                        : () {
                            setState(() => _isEditing = false);
                            if (_draft.trim() != wing) widget.onRename(_draft.trim());
                          },
                    child: const Text('Save', style: TextStyle(color: AppColors.tealPrimary)),
                  ),
                ]
              : [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Wing $wing', style: tt.bodyMedium?.copyWith(fontWeight: FontWeight.bold)),
                        Text(
                          '${widget.unitCount} unit(s)${widget.isStray ? ' • not on the wing list' : ''}',
                          style: tt.bodySmall?.copyWith(color: widget.isStray ? cs.error : cs.onSurfaceVariant),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    key: Key('edit_wing_btn_$wing'),
                    tooltip: 'Rename wing',
                    onPressed: () => setState(() {
                      _draft = wing;
                      _isEditing = true;
                    }),
                    icon: const Icon(Icons.edit, color: AppColors.tealPrimary),
                  ),
                  if (widget.onDelete != null)
                    IconButton(
                      key: Key('delete_wing_btn_$wing'),
                      tooltip: 'Remove wing',
                      onPressed: _confirmDelete,
                      icon: Icon(Icons.delete, color: cs.error),
                    ),
                ],
        ),
      ),
    );
  }
}

// ------------------------------------------------------ step 3: floor inventory

bool _sameStrings(List<String> a, List<String> b) {
  if (a.length != b.length) return false;
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) return false;
  }
  return true;
}

bool _samePlan(WingPlan? a, WingPlan? b) {
  if (identical(a, b)) return true;
  if (a == null || b == null) return false;
  if (a.name != b.name || a.totalFloors != b.totalFloors || a.unitCounts.length != b.unitCounts.length) {
    return false;
  }
  for (var i = 0; i < a.unitCounts.length; i++) {
    if (a.unitCounts[i] != b.unitCounts[i]) return false;
  }
  return true;
}

class FloorInventoryStep extends StatefulWidget {
  const FloorInventoryStep({
    super.key,
    required this.language,
    required this.wingNames,
    required this.wingPlans,
    required this.onWingPlans,
  });

  final AppLanguage language;
  final List<String> wingNames;
  final List<WingPlan> wingPlans;
  final ValueChanged<List<WingPlan>> onWingPlans;

  @override
  State<FloorInventoryStep> createState() => _FloorInventoryStepState();
}

class _FloorInventoryStepState extends State<FloorInventoryStep> {
  // The wing being configured: the first one still unconfigured, so finishing
  // a wing rolls straight on to the next one.
  late String _selectedWing;
  WingPlan? _openingPlan;
  String _floors = '';
  List<WingUnitCount> _unitCounts = const [];

  // Opening on a wing that is already configured - which is what the Society
  // Profile does - shows that wing's numbers rather than an empty form.
  WingPlan? _computeOpeningPlan() {
    final first = widget.wingNames.isEmpty ? null : widget.wingNames.first;
    for (final p in widget.wingPlans) {
      if (p.name == first) return p;
    }
    return null;
  }

  void _loadFrom(WingPlan? plan) {
    _floors = (plan != null && plan.totalFloors > 0) ? plan.totalFloors.toString() : '';
    _unitCounts = plan?.unitCounts ?? const [];
  }

  @override
  void initState() {
    super.initState();
    _selectedWing = widget.wingNames.isEmpty ? '' : widget.wingNames.first;
    _openingPlan = _computeOpeningPlan();
    _loadFrom(_openingPlan);
  }

  @override
  void didUpdateWidget(covariant FloorInventoryStep oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!_sameStrings(oldWidget.wingNames, widget.wingNames)) {
      _selectedWing = widget.wingNames.isEmpty ? '' : widget.wingNames.first;
    }
    final opening = _computeOpeningPlan();
    if (!_samePlan(opening, _openingPlan)) {
      _openingPlan = opening;
      _loadFrom(opening);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final wingNames = widget.wingNames;
    final wingPlans = widget.wingPlans;

    final floorCount = int.tryParse(_floors) ?? 0;
    final plannedUnits = _unitCounts.fold<int>(0, (s, c) => s + (c.quantity < 0 ? 0 : c.quantity));
    final isPlanValid = _selectedWing.trim().isNotEmpty &&
        plannedUnits > 0 &&
        _unitCounts.every((c) => c.unitType.trim().isNotEmpty);
    final remaining = wingNames.where((n) => !wingPlans.any((p) => p.name == n) && n != _selectedWing).length;

    List<String>? sample;
    if (isPlanValid) {
      sample = WingPlan(name: _selectedWing, totalFloors: floorCount, unitCounts: _unitCounts).generateUnitNumbers();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        StepTitle(tr('wizard_step3', widget.language)),
        SearchableDropdownField(
          label: 'Select Wing (विंग निवडा)',
          value: _selectedWing,
          options: wingNames,
          onValueChange: (picked) {
            WingPlan? existing;
            for (final p in wingPlans) {
              if (p.name == picked) existing = p;
            }
            setState(() {
              _selectedWing = picked;
              _loadFrom(existing);
            });
          },
          allowFreeText: false,
          testTag: 'wizard_wing_select_dropdown',
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_total_floors_input'),
          value: _floors,
          onChanged: (v) => setState(() => _floors = _digitsOnly(v, 3)),
          label: 'Total Floors',
          keyboardType: TextInputType.number,
        ),
        const SizedBox(height: 16),
        WingUnitCountsEditor(
          wingName: _selectedWing,
          unitCounts: _unitCounts,
          onUnitCounts: (v) => setState(() => _unitCounts = v),
        ),
        if (sample != null) ...[
          const SizedBox(height: 8),
          Text(
            '${sample.length} units will be created: ${sample.take(4).join(', ')}'
            '${sample.length > 4 ? ' … ${sample.last}' : ''}',
            style: tt.bodySmall?.copyWith(color: AppColors.tealPrimary),
          ),
        ],
        const SizedBox(height: 10),
        FilledButton(
          key: const Key('wizard_save_wing_btn'),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.tealPrimary,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
          onPressed: !isPlanValid
              ? null
              : () {
                  final updated = [
                    ...wingPlans.where((p) => p.name != _selectedWing),
                    WingPlan(
                      name: _selectedWing,
                      totalFloors: floorCount,
                      unitCounts: _unitCounts.where((c) => c.unitType.trim().isNotEmpty).toList(),
                    ),
                  ];
                  // Stable sort by the wing's position in the wing list.
                  final indexed = [for (var i = 0; i < updated.length; i++) (i, updated[i])];
                  indexed.sort((a, b) {
                    final c = wingNames.indexOf(a.$2.name).compareTo(wingNames.indexOf(b.$2.name));
                    return c != 0 ? c : a.$1.compareTo(b.$1);
                  });
                  widget.onWingPlans([for (final e in indexed) e.$2]);

                  // Move on to the next wing that still needs configuring.
                  String? next;
                  for (final name in wingNames) {
                    if (!updated.any((p) => p.name == name)) {
                      next = name;
                      break;
                    }
                  }
                  if (next != null) {
                    setState(() {
                      _selectedWing = next!;
                      _floors = '';
                      _unitCounts = const [];
                    });
                  }
                },
          child: Text(remaining > 0 ? 'Save Wing & Configure Next ($remaining left)' : 'Save Wing $_selectedWing'),
        ),
        if (wingPlans.isNotEmpty) ...[
          const SizedBox(height: 14),
          const SubHeading('Configured Wings'),
          const SizedBox(height: 6),
          for (final plan in wingPlans)
            Card(
              color: AppColors.surfaceVariant,
              elevation: 0,
              margin: const EdgeInsets.only(bottom: 6),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Wing ${plan.name} • ${plan.totalUnits} units',
                            style: tt.bodyMedium?.copyWith(fontWeight: FontWeight.bold),
                          ),
                          Text(
                            '${plan.totalFloors} floors • '
                            '${plan.unitCounts.map((c) => '${c.unitType} × ${c.quantity}').join(', ')}',
                            style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      tooltip: 'Remove wing',
                      onPressed: () => widget.onWingPlans(wingPlans.where((p) => p.name != plan.name).toList()),
                      icon: Icon(Icons.delete, color: cs.error),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ],
    );
  }
}

/// The unit types one wing holds, and how many units of each.
///
/// This is what decides how many flats the wing is expanded into - the monthly
/// tariff for each type is set later, under Maintenance > Tariff.
class WingUnitCountsEditor extends StatefulWidget {
  const WingUnitCountsEditor({
    super.key,
    required this.wingName,
    required this.unitCounts,
    required this.onUnitCounts,
  });

  final String wingName;
  final List<WingUnitCount> unitCounts;
  final ValueChanged<List<WingUnitCount>> onUnitCounts;

  @override
  State<WingUnitCountsEditor> createState() => _WingUnitCountsEditorState();
}

class _WingUnitCountsEditorState extends State<WingUnitCountsEditor> {
  String _newTypeName = '';

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final unitCounts = widget.unitCounts;
    final total = unitCounts.fold<int>(0, (s, c) => s + (c.quantity < 0 ? 0 : c.quantity));
    final canAdd = _newTypeName.trim().isNotEmpty &&
        !unitCounts.any((c) => c.unitType.toLowerCase() == _newTypeName.trim().toLowerCase());

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SubHeading(
          widget.wingName.trim().isEmpty
              ? 'Unit Types & Quantity'
              : 'Wing ${widget.wingName} - Unit Types & Quantity',
        ),
        Text(
          'Which unit types this wing has, and how many units of each.',
          style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
        ),
        const SizedBox(height: 8),
        for (var index = 0; index < unitCounts.length; index++)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              children: [
                Expanded(
                  flex: 12,
                  child: StateTextField(
                    value: unitCounts[index].unitType,
                    onChanged: (renamed) {
                      final list = [...unitCounts];
                      list[index] = list[index].copyWith(unitType: renamed);
                      widget.onUnitCounts(list);
                    },
                    label: 'Unit Type',
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  flex: 10,
                  child: StateTextField(
                    key: Key('wizard_unit_quantity_${unitCounts[index].unitType}'),
                    value: unitCounts[index].quantity > 0 ? unitCounts[index].quantity.toString() : '',
                    onChanged: (raw) {
                      final quantity = int.tryParse(_digitsOnly(raw, 4)) ?? 0;
                      final list = [...unitCounts];
                      list[index] = list[index].copyWith(quantity: quantity);
                      widget.onUnitCounts(list);
                    },
                    label: 'Quantity',
                    keyboardType: TextInputType.number,
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  tooltip: 'Remove unit type',
                  onPressed: () => widget.onUnitCounts([
                    for (var i = 0; i < unitCounts.length; i++)
                      if (i != index) unitCounts[i],
                  ]),
                  icon: Icon(Icons.delete, color: cs.error),
                ),
              ],
            ),
          ),
        Row(
          children: [
            Expanded(
              child: StateTextField(
                key: const Key('wizard_new_unit_type_input'),
                value: _newTypeName,
                onChanged: (v) => setState(() => _newTypeName = v),
                label: 'Add Unit Type',
                placeholder: '2 BHK',
              ),
            ),
            const SizedBox(width: 8),
            FilledButton(
              key: const Key('wizard_add_unit_type_btn'),
              style: FilledButton.styleFrom(
                backgroundColor: AppColors.tealPrimary,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              onPressed: !canAdd
                  ? null
                  : () {
                      widget.onUnitCounts([...unitCounts, WingUnitCount(unitType: _newTypeName.trim(), quantity: 0)]);
                      setState(() => _newTypeName = '');
                    },
              child: const Icon(Icons.add, semanticLabel: 'Add unit type'),
            ),
          ],
        ),
        if (total > 0) ...[
          const SizedBox(height: 6),
          Text(
            '$total unit(s) in this wing',
            key: const Key('wizard_wing_total_units'),
            style: tt.bodySmall?.copyWith(fontWeight: FontWeight.bold, color: AppColors.tealPrimary),
          ),
        ],
      ],
    );
  }
}

// ------------------------------------------------------- step 4: office bearers

class _OfficeBearersStep extends StatefulWidget {
  const _OfficeBearersStep({
    required this.language,
    required this.name,
    required this.onName,
    required this.flat,
    required this.onFlat,
    required this.phone,
    required this.onPhone,
    required this.email,
    required this.onEmail,
    required this.pin,
    required this.onPin,
    required this.confirmPin,
    required this.onConfirmPin,
  });

  final AppLanguage language;
  final String name;
  final ValueChanged<String> onName;
  final String flat;
  final ValueChanged<String> onFlat;
  final String phone;
  final ValueChanged<String> onPhone;
  final String email;
  final ValueChanged<String> onEmail;
  final String pin;
  final ValueChanged<String> onPin;
  final String confirmPin;
  final ValueChanged<String> onConfirmPin;

  @override
  State<_OfficeBearersStep> createState() => _OfficeBearersStepState();
}

class _OfficeBearersStepState extends State<_OfficeBearersStep> {
  bool _isPinVisible = false;

  @override
  Widget build(BuildContext context) {
    final w = widget;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        StepTitle(tr('wizard_step4', w.language)),
        StateTextField(
          key: const Key('wizard_chairman_name_input'),
          value: w.name,
          onChanged: w.onName,
          label: 'Chairman Full Name (अध्यक्षांचे नाव)',
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_chairman_flat_input'),
          value: w.flat,
          onChanged: (v) => w.onFlat(v.toUpperCase()),
          label: 'Chairman Unit No. (सदनिका क्र.)',
          placeholder: 'e.g. A-101',
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_chairman_phone_input'),
          value: w.phone,
          onChanged: (v) => w.onPhone(_digitsOnly(v, 10)),
          label: 'Chairman Mobile (मोबाईल)',
          leadingIcon: const Icon(Icons.phone, color: _tealIcon),
          keyboardType: TextInputType.phone,
        ),
        const FieldGap(),
        StateTextField(
          value: w.email,
          onChanged: w.onEmail,
          label: 'Email (ईमेल)',
          leadingIcon: const Icon(Icons.email, color: _tealIcon),
          keyboardType: TextInputType.emailAddress,
        ),
        const FieldGap(),
        // Exactly four digits - this is the PIN the chairman logs in with.
        StateTextField(
          key: const Key('wizard_pin_input'),
          value: w.pin,
          onChanged: (v) => w.onPin(_digitsOnly(v, 4)),
          label: 'Set 4-Digit Login PIN (लॉगिन पिन)',
          supportingText: '${w.pin.length}/4 digits',
          isError: w.pin.isNotEmpty && w.pin.length != 4,
          leadingIcon: const Icon(Icons.lock, color: _tealIcon),
          trailingIcon: PinVisibilityToggle(
            isVisible: _isPinVisible,
            onToggle: () => setState(() => _isPinVisible = !_isPinVisible),
            testTag: 'wizard_pin_visibility_btn',
          ),
          obscureText: !_isPinVisible,
          keyboardType: TextInputType.number,
        ),
        const FieldGap(),
        StateTextField(
          key: const Key('wizard_confirm_pin_input'),
          value: w.confirmPin,
          onChanged: (v) => w.onConfirmPin(_digitsOnly(v, 4)),
          label: 'Confirm PIN (पिन पुन्हा टाका)',
          leadingIcon: const Icon(Icons.lock, color: _tealIcon),
          obscureText: !_isPinVisible,
          keyboardType: TextInputType.number,
          isError: w.confirmPin.isNotEmpty && w.confirmPin != w.pin,
        ),
      ],
    );
  }
}

// -------------------------------------------------------- step 5: confirmation

class _ConfirmationStep extends StatelessWidget {
  const _ConfirmationStep({
    required this.language,
    required this.societyName,
    required this.societyCode,
    required this.regNumber,
    required this.location,
    required this.address,
    required this.pinCode,
    required this.societyPhone,
    required this.societyEmail,
    required this.logoBase64,
    required this.wingNames,
    required this.wingPlans,
    required this.unitTypes,
    required this.plannedUnits,
    required this.chairmanName,
    required this.chairmanFlat,
    required this.chairmanPhone,
    required this.chairmanEmail,
    required this.bankName,
    required this.initialBankBalance,
  });

  final AppLanguage language;
  final String societyName;
  final String societyCode;
  final String regNumber;
  final String location;
  final String address;
  final String pinCode;
  final String societyPhone;
  final String societyEmail;
  final String logoBase64;
  final List<String> wingNames;
  final List<WingPlan> wingPlans;
  final List<UnitTypeConfig> unitTypes;
  final int plannedUnits;
  final String chairmanName;
  final String chairmanFlat;
  final String chairmanPhone;
  final String chairmanEmail;
  final String bankName;
  final String initialBankBalance;

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    String orDash(String s) => s.trim().isEmpty ? '-' : s;
    final typesText = unitTypes
        .where((t) => t.name.trim().isNotEmpty)
        .map((t) => t.monthlyTariff > 0 ? '${t.name} (₹${t.monthlyTariff.toInt()})' : t.name)
        .join(', ');
    const divider = Divider(height: 20);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        StepTitle(tr('wizard_step5', language)),
        Card(
          color: AppColors.surfaceVariant,
          elevation: 0,
          margin: EdgeInsets.zero,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    SocietyLogoMark(logoBase64: logoBase64, size: 48),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(societyName, style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                          Text(
                            'Code: $societyCode${regNumber.trim().isNotEmpty ? ' | Reg: $regNumber' : ''}',
                            style: tt.bodySmall,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                divider,
                if (location.trim().isNotEmpty) _SummaryLine('Location', location),
                if (address.trim().isNotEmpty)
                  _SummaryLine('Address', address + (pinCode.trim().isNotEmpty ? ' - $pinCode' : '')),
                if (societyPhone.trim().isNotEmpty) _SummaryLine('Contact', societyPhone),
                if (societyEmail.trim().isNotEmpty) _SummaryLine('Email', societyEmail),
                divider,
                _SummaryLine('Wings', orDash(wingNames.join(', '))),
                _SummaryLine(
                  'Floor inventory',
                  orDash(wingPlans.map((p) => '${p.name}: ${p.totalFloors} floors, ${p.totalUnits} units').join(' | ')),
                ),
                _SummaryLine('Units to create', plannedUnits.toString()),
                _SummaryLine('Unit types', orDash(typesText)),
                divider,
                _SummaryLine('Chairman', '$chairmanName ($chairmanFlat)'),
                _SummaryLine('Chairman mobile', chairmanPhone),
                if (chairmanEmail.trim().isNotEmpty) _SummaryLine('Chairman email', chairmanEmail),
                if (bankName.trim().isNotEmpty)
                  _SummaryLine(
                    'Bank',
                    bankName + (initialBankBalance.trim().isNotEmpty ? ' (₹$initialBankBalance)' : ''),
                  ),
                divider,
                Text(
                  'On finish, the login details are sent to the chairman and the flats above are created in the Resident Directory.',
                  style: tt.bodySmall?.copyWith(color: AppColors.tealPrimary),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

/// Shown the moment the society exists. Hands the login details to the chairman
/// over SMS or email before anyone is signed in.
class RegistrationSuccessDialog extends StatefulWidget {
  const RegistrationSuccessDialog({
    super.key,
    required this.society,
    required this.chairman,
    required this.body,
    required this.language,
    required this.onContinue,
  });

  final SocietyInfo society;
  final Resident chairman;
  final String body;
  final AppLanguage language;
  final VoidCallback onContinue;

  @override
  State<RegistrationSuccessDialog> createState() => _RegistrationSuccessDialogState();
}

class _RegistrationSuccessDialogState extends State<RegistrationSuccessDialog> {
  String? _sendError;

  Future<void> _launch(Uri uri, String failure) async {
    var ok = false;
    try {
      ok = await launchUrl(uri, mode: LaunchMode.externalApplication);
    } catch (_) {
      ok = false;
    }
    if (mounted) setState(() => _sendError = ok ? null : failure);
  }

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    final cs = Theme.of(context).colorScheme;
    final language = widget.language;
    final chairman = widget.chairman;
    final buttonStyle = OutlinedButton.styleFrom(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      side: const BorderSide(color: AppColors.tealPrimary),
    );

    return AlertDialog(
      insetPadding: dialogInsetPadding(context),
      icon: const Icon(Icons.check_circle, color: AppColors.tealPrimary),
      title: Text(tr('registration_done', language), style: tt.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
      content: SizedBox(
        width: double.maxFinite,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Card(
                color: AppColors.surfaceVariant,
                elevation: 0,
                margin: EdgeInsets.zero,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Text(widget.body, style: tt.bodySmall),
                ),
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                key: const Key('send_chairman_sms_btn'),
                style: buttonStyle,
                onPressed: () => _launch(
                  Uri.parse('smsto:${chairman.contactNumber.trim()}?body=${Uri.encodeComponent(widget.body)}'),
                  'No messaging app available on this device.',
                ),
                icon: const Icon(Icons.sms, color: AppColors.tealPrimary),
                label: Text(
                  tr('send_to_chairman_sms', language),
                  style: const TextStyle(color: AppColors.tealPrimary),
                ),
              ),
              if (chairman.email.trim().isNotEmpty) ...[
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  key: const Key('send_chairman_email_btn'),
                  style: buttonStyle,
                  onPressed: () => _launch(
                    Uri.parse(
                      'mailto:${chairman.email.trim()}'
                      '?subject=${Uri.encodeComponent('SocietySarv login details - ${widget.society.name}')}'
                      '&body=${Uri.encodeComponent(widget.body)}',
                    ),
                    'No email app available on this device.',
                  ),
                  icon: const Icon(Icons.email, color: AppColors.tealPrimary),
                  label: Text(
                    tr('send_to_chairman_email', language),
                    style: const TextStyle(color: AppColors.tealPrimary),
                  ),
                ),
              ],
              if (_sendError != null) ...[
                const SizedBox(height: 8),
                Text(_sendError!, style: tt.bodySmall?.copyWith(color: cs.error)),
              ],
            ],
          ),
        ),
      ),
      actions: [
        FilledButton(
          key: const Key('registration_continue_btn'),
          style: FilledButton.styleFrom(backgroundColor: AppColors.tealPrimary),
          onPressed: widget.onContinue,
          child: const Text('Continue'),
        ),
      ],
    );
  }
}

// -------------------------------------------------------------- small helpers

class StepTitle extends StatelessWidget {
  const StepTitle(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Text(
        text,
        style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold, color: theme.colorScheme.onSurface),
      ),
    );
  }
}

class SubHeading extends StatelessWidget {
  const SubHeading(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Text(
      text,
      style: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold, color: theme.colorScheme.onSurface),
    );
  }
}

class FieldGap extends StatelessWidget {
  const FieldGap({super.key});

  @override
  Widget build(BuildContext context) => const SizedBox(height: 10);
}

class _SummaryLine extends StatelessWidget {
  const _SummaryLine(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 2),
      child: Text.rich(
        TextSpan(
          children: [
            TextSpan(text: '$label: ', style: tt.bodySmall?.copyWith(fontWeight: FontWeight.bold)),
            TextSpan(text: value, style: tt.bodySmall),
          ],
        ),
      ),
    );
  }
}
