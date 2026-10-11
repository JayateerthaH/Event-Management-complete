import { jwtDecode } from 'jwt-decode';

/**
 * Role priority — higher index = higher priority.
 * ADMIN > ORGANIZER > STUDENT
 */
const ROLE_PRIORITY = ['STUDENT', 'ORGANIZER', 'ADMIN'];

/**
 * Normalise a single role string to uppercase, stripping the ROLE_ prefix
 * that Spring Security often adds.
 */
const normalise = (role) => {
  if (typeof role !== 'string') return '';
  return role.toUpperCase().replace(/^ROLE_/, '');
};

/**
 * Extract the roles array from the user object stored in auth context.
 * Handles multiple common shapes returned by different backends.
 *
 * @param {object|null} user – user object from AuthContext
 * @returns {string[]} normalised roles (e.g. ["STUDENT"])
 */
export const extractRoles = (user) => {
  if (!user) return [];

  // Direct roles array
  if (Array.isArray(user.roles) && user.roles.length) {
    return user.roles.map(normalise).filter(Boolean);
  }

  // Single role string
  if (typeof user.role === 'string') {
    const r = normalise(user.role);
    return r ? [r] : [];
  }

  // authorities array (Spring Security format)
  if (Array.isArray(user.authorities)) {
    return user.authorities
      .map((a) => normalise(typeof a === 'string' ? a : a?.authority))
      .filter(Boolean);
  }

  return [];
};

/**
 * Try to read roles from the JWT access token stored in localStorage.
 *
 * @returns {string[]} normalised roles
 */
export const extractRolesFromToken = () => {
  try {
    const token = localStorage.getItem('jwt_token');
    if (!token) return [];
    const decoded = jwtDecode(token);

    if (Array.isArray(decoded.roles)) return decoded.roles.map(normalise).filter(Boolean);
    if (typeof decoded.role === 'string') {
      const r = normalise(decoded.role);
      return r ? [r] : [];
    }
    if (Array.isArray(decoded.authorities)) {
      return decoded.authorities
        .map((a) => normalise(typeof a === 'string' ? a : a?.authority))
        .filter(Boolean);
    }
  } catch {
    /* token missing / malformed — fail silently */
  }
  return [];
};

/**
 * Given a user object (and optionally the raw JWT), return the highest
 * priority role the user possesses.
 *
 * @param {object|null} user
 * @returns {'ADMIN'|'ORGANIZER'|'STUDENT'|null}
 */
export const resolveRole = (user) => {
  let roles = extractRoles(user);

  // Fallback: try reading from the JWT directly
  if (!roles.length) {
    roles = extractRolesFromToken();
  }

  if (!roles.length) return null;

  // Return the role with the highest priority
  let best = null;
  let bestIdx = -1;

  for (const role of roles) {
    const idx = ROLE_PRIORITY.indexOf(role);
    if (idx > bestIdx) {
      best = role;
      bestIdx = idx;
    }
  }

  return best;
};

export default resolveRole;
