let hasShownMemberPopup = false;

export function claimMemberPopup() {
  if (hasShownMemberPopup) {
    return false;
  }

  hasShownMemberPopup = true;
  return true;
}
export function resetMemberPopup() {
  hasShownMemberPopup = false;
}