import React from 'react';
import {Img, staticFile} from 'remotion';
import {C, F} from '../lib';

/** Real phone screenshots are 720×1600 (Oppo CPH1937). Overlays inside the screen use these coordinates. */
export const SHOT_W = 720;
export const SHOT_H = 1600;
const STATUS_H = 64;

/** Clean status bar painted over the captured one (the real capture shows personal notification icons). */
const CleanStatusBar: React.FC = () => (
	<div
		style={{
			position: 'absolute',
			left: 0,
			top: 0,
			width: SHOT_W,
			height: STATUS_H,
			background: 'linear-gradient(180deg, #111324 0%, #16172C 100%)',
			display: 'flex',
			alignItems: 'center',
			justifyContent: 'space-between',
			padding: '0 38px 0 44px',
			boxSizing: 'border-box',
			color: C.head,
			fontFamily: F.sans,
			fontWeight: 600,
			fontSize: 27,
		}}
	>
		<span>9:41</span>
		<svg width="118" height="28" viewBox="0 0 118 28">
			{/* signal */}
			{[0, 1, 2, 3].map((i) => (
				<rect key={i} x={i * 9} y={20 - i * 5} width="6" height={8 + i * 5} rx="1.5" fill={C.head} />
			))}
			{/* wifi */}
			<path d="M52 12a16 16 0 0 1 22 0M56 16.5a10 10 0 0 1 14 0M60 21a4 4 0 0 1 6 0" stroke={C.head} strokeWidth="2.8" fill="none" strokeLinecap="round" />
			{/* battery */}
			<rect x="82" y="6" width="30" height="16" rx="4" stroke={C.head} strokeWidth="2.4" fill="none" />
			<rect x="85" y="9" width="21" height="10" rx="2" fill={C.lime} />
			<rect x="113.5" y="11" width="3" height="6" rx="1" fill={C.head} />
		</svg>
	</div>
);

/**
 * Phone frame showing a real app screenshot. `height` is the outer device height in px.
 * `children` render inside the screen in 720×1600 screenshot coordinates (lasers, callouts).
 */
export const Phone: React.FC<{
	src: string;
	height: number;
	rotateX?: number;
	rotateY?: number;
	rotateZ?: number;
	glow?: number;
	children?: React.ReactNode;
	style?: React.CSSProperties;
}> = ({src, height, rotateX = 0, rotateY = 0, rotateZ = 0, glow = 0.25, children, style}) => {
	const bezel = height * 0.018;
	const screenH = height - bezel * 2;
	const screenW = screenH * (SHOT_W / SHOT_H);
	const width = screenW + bezel * 2;
	const k = screenW / SHOT_W;
	const radius = height * 0.06;
	return (
		<div style={{perspective: height * 2.4, ...style}}>
			<div
				style={{
					width,
					height,
					borderRadius: radius,
					background: 'linear-gradient(145deg, #2E2C4E 0%, #0C0D1A 45%, #1A1830 100%)',
					padding: bezel,
					boxSizing: 'border-box',
					transformStyle: 'preserve-3d',
					transform: `rotateX(${rotateX}deg) rotateY(${rotateY}deg) rotateZ(${rotateZ}deg)`,
					boxShadow: `0 ${height * 0.05}px ${height * 0.12}px rgba(4,5,14,0.7), 0 0 ${height * 0.08}px rgba(221,248,61,${glow * 0.35}), inset 0 0 0 2px rgba(255,255,255,0.14)`,
				}}
			>
				<div style={{position: 'relative', width: screenW, height: screenH, borderRadius: radius - bezel, overflow: 'hidden', background: C.nadir}}>
					<div style={{position: 'absolute', left: 0, top: 0, width: SHOT_W, height: SHOT_H, transformOrigin: '0 0', scale: String(k)}}>
						<Img src={staticFile(src)} style={{position: 'absolute', left: 0, top: 0, width: SHOT_W, height: SHOT_H}} />
						<CleanStatusBar />
						{children}
					</div>
					{/* glass reflection sweep */}
					<div
						style={{
							position: 'absolute',
							inset: 0,
							background: 'linear-gradient(115deg, rgba(255,255,255,0.10) 0%, rgba(255,255,255,0) 32%)',
							pointerEvents: 'none',
						}}
					/>
				</div>
			</div>
		</div>
	);
};
