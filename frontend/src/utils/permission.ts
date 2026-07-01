export function hasPermission(permissionCodes: string[] | undefined, code: string): boolean {
  if (!permissionCodes || permissionCodes.length === 0) return false
  return permissionCodes.includes(code)
}

export function hasAnyPermission(permissionCodes: string[] | undefined, codes: string[]): boolean {
  if (!permissionCodes || permissionCodes.length === 0) return false
  return codes.some((code) => permissionCodes.includes(code))
}

export function hasAllPermissions(permissionCodes: string[] | undefined, codes: string[]): boolean {
  if (!permissionCodes || permissionCodes.length === 0) return false
  return codes.every((code) => permissionCodes.includes(code))
}
