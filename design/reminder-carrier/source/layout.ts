export const board = { width: "52mm", height: "26mm" };
export const primarySide = "top" as const; // Faces the front of the radio.
export const serviceSide = "bottom" as const;
export const keys = {
  sound: { x: 0, y: -4 },
  power: { x: 7, y: 9 },
  up: { x: -18, y: 10 }, down: { x: -7, y: 10 },
};
export const chargeUsb = { x: 17.4, y: 9.0, rotation: 0 };
export const amp = { x: 17, y: 2 };
export const OPENING_ROTATION_DEG = {up:0,left:90,down:180,right:270} as const;
export type OpeningDirection = keyof typeof OPENING_ROTATION_DEG;
export function rotationForOpening(nativeOpening: OpeningDirection,targetOpening: OpeningDirection):number {
 return (OPENING_ROTATION_DEG[targetOpening]-OPENING_ROTATION_DEG[nativeOpening]+360)%360;
}
