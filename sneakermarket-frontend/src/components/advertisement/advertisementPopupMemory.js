let hasShownMemberPopup = false;

export function claimMemberPopup() {
  if (hasShownMemberPopup) {
    return false;
  }

  hasShownMemberPopup = true;
  return true;
}