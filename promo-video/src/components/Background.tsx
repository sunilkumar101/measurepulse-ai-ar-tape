import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame} from 'remotion';
import {C} from '../lib';

/** Twilight architectural photo, cover-fit, with the app's gradient wash and a vignette. */
export const PhotoBackground: React.FC<{
	src: string;
	scale?: number;
	x?: number;
	y?: number;
	blur?: number;
	dim?: number;
	opacity?: number;
}> = ({src, scale = 1.06, x = 0, y = 0, blur = 0, dim = 0.45, opacity = 1}) => (
	<AbsoluteFill style={{opacity, overflow: 'hidden'}}>
		<Img
			src={staticFile(src)}
			style={{
				position: 'absolute',
				inset: 0,
				width: '100%',
				height: '100%',
				objectFit: 'cover',
				scale: String(scale),
				translate: `${x}px ${y}px`,
				filter: blur > 0 ? `blur(${blur}px) saturate(1.1)` : 'saturate(1.08)',
			}}
		/>
		<AbsoluteFill
			style={{
				background: `linear-gradient(180deg, rgba(39,36,70,${dim * 0.8}) 0%, rgba(28,30,54,${dim}) 55%, rgba(17,19,36,${Math.min(1, dim + 0.35)}) 100%)`,
			}}
		/>
	</AbsoluteFill>
);

/** The app background gradient #272446 → #1C1E36 → #111324 with a soft orchid bloom. */
export const TwilightGradient: React.FC = () => (
	<AbsoluteFill
		style={{
			background: `radial-gradient(ellipse at 30% 18%, rgba(109,58,156,0.45) 0%, transparent 55%), radial-gradient(ellipse at 85% 85%, rgba(184,69,237,0.18) 0%, transparent 50%), linear-gradient(180deg, ${C.zenith} 0%, ${C.mid} 50%, ${C.nadir} 100%)`,
		}}
	/>
);

export const Vignette: React.FC<{strength?: number}> = ({strength = 0.6}) => (
	<AbsoluteFill style={{background: `radial-gradient(ellipse at center, transparent 45%, rgba(5,6,16,${strength}) 100%)`, pointerEvents: 'none'}} />
);

/** Film grain, reseeded on twos so nothing is ever dead-still. */
export const Grain: React.FC<{opacity?: number}> = ({opacity = 0.08}) => {
	const frame = useCurrentFrame();
	const seed = Math.floor(frame / 2) % 97;
	return (
		<AbsoluteFill style={{opacity, mixBlendMode: 'overlay', pointerEvents: 'none'}}>
			<svg width="100%" height="100%">
				<filter id={`grain-${seed}`}>
					<feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" seed={seed} stitchTiles="stitch" />
					<feColorMatrix type="saturate" values="0" />
				</filter>
				<rect width="100%" height="100%" filter={`url(#grain-${seed})`} />
			</svg>
		</AbsoluteFill>
	);
};

/** Faint lime dot grid drifting a few px per second (spatial "mesh" texture). */
export const DotGrid: React.FC<{opacity?: number}> = ({opacity = 0.12}) => {
	const frame = useCurrentFrame();
	const drift = (frame / 60) * 6;
	return (
		<AbsoluteFill
			style={{
				opacity,
				backgroundImage: 'radial-gradient(rgba(221,248,61,0.9) 1.2px, transparent 1.4px)',
				backgroundSize: '44px 44px',
				backgroundPosition: `${drift}px ${drift * 0.6}px`,
				maskImage: 'radial-gradient(ellipse at center, black 20%, transparent 75%)',
				pointerEvents: 'none',
			}}
		/>
	);
};
