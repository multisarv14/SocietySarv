#!/usr/bin/env bash
# Deploy guard for firestore.rules.
#
# The strict rules require `request.auth != null`, which the app can only
# satisfy once Anonymous Auth is enabled in the Firebase console. Deploying them
# before that flips every read and write to PERMISSION_DENIED and takes the
# whole app down, so refuse the deploy instead of letting it happen.
set -euo pipefail

RULES="${1:-firestore.rules}"
API_KEY="AIzaSyCjodhNA-w07k72T_tpyMZ8YT99veKOngg"

if ! grep -q 'return request.auth != null' "$RULES"; then
  echo "✔  $RULES does not require an authenticated identity - nothing to check."
  exit 0
fi

echo "i  $RULES requires request.auth != null - verifying Anonymous Auth is enabled..."
response=$(curl -s -X POST \
  "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${API_KEY}" \
  -H "Content-Type: application/json" -d '{"returnSecureToken":true}')

if echo "$response" | grep -q '"idToken"'; then
  echo "✔  Anonymous Auth is enabled - safe to deploy the strict rules."
  exit 0
fi

cat >&2 <<'MSG'

✖  DEPLOY BLOCKED - Anonymous Auth is still disabled.

   firestore.rules requires `request.auth != null`, but the app cannot get an
   identity yet, so deploying these rules would break every screen with
   PERMISSION_DENIED.

   Fix it in this order:
     1. Firebase Console -> Authentication -> Sign-in method -> Anonymous -> Enable
     2. Re-run this deploy (the guard will then pass)

   To go back to the permissive rules instead:
     sed -i '' 's/return request.auth != null;.*/return true; \/\/ Anonymous Auth still off/' firestore.rules

MSG
exit 1
