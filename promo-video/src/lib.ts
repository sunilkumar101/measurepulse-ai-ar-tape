import {Easing, interpolate, spring} from 'remotion';
import timeline from './timeline.json';

/** Beats in seconds — the storyboard. Never hardcode a start time in a component. */
export const TL = timeline;

/** Frosted Twilight Glass & Electric Lime — the app's own design tokens. */
export const C = {
	zenith: '#272446',
	mid: '#1C1E36',
	nadir: '#111324',
	lime: '#DDF83D',
	ink: '#111322',
	ice: '#E2E8F0',
	head: '#F3F4FB',
	muted: '#9E9FB9',
	magenta: '#B845ED',
	orchid: '#6D3A9C',
	cyan: '#5EE7F2',
	alert: '#FF4D8D',
	glass: 'rgba(18, 21, 42, 0.68)',
	rim: 'rgba(255, 255, 255, 0.28)',
	limeGlow: 'rgba(221, 248, 61, 0.45)',
} as const;

export const F = {
	sans: 'Plus Jakarta Sans',
	mono: 'JetBrains Mono',
} as const;

export const easeOut = Easing.bezier(0.16, 1, 0.3, 1);
export const easeIn = Easing.bezier(0.6, 0, 0.9, 0.35);
export const easeInOut = Easing.bezier(0.65, 0, 0.35, 1);
export const overshoot = Easing.bezier(0.55, 0, 0.2, 1.12);

/** 0→1 tween between two times (seconds). */
export const prog = (t: number, t0: number, t1: number, ease = easeOut) =>
	interpolate(t, [t0, t1], [0, 1], {
		extrapolateLeft: 'clamp',
		extrapolateRight: 'clamp',
		easing: ease,
	});

export type SpringFeel = {damping: number; stiffness: number; mass: number};
export const SETTLE: SpringFeel = {damping: 14, stiffness: 140, mass: 0.8};
export const HEAVY: SpringFeel = {damping: 18, stiffness: 180, mass: 0.9};
export const POP: SpringFeel = {damping: 10, stiffness: 220, mass: 0.7};
export const NOTCH: SpringFeel = {damping: 12, stiffness: 190, mass: 0.75};

/** Spring that starts at `startSec` (local scene time). */
export const sp = (frame: number, fps: number, startSec: number, config: SpringFeel = SETTLE) =>
	spring({frame: Math.max(0, frame - Math.round(startSec * fps)), fps, config});

/** Deterministic pseudo-random in [0,1). */
export const rand = (n: number) => {
	const x = Math.sin(n * 12.9898 + 78.233) * 43758.5453;
	return x - Math.floor(x);
};

export const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

/** Scene envelope: 1 while on screen, eases out over the last `exitDur` seconds before `exitAt`. */
export const exitOut = (t: number, exitAt: number, exitDur = 0.4) => 1 - prog(t, exitAt, exitAt + exitDur, easeIn);
