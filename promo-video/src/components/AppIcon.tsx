import React from 'react';
import {C} from '../lib';

/**
 * The launcher icon (res/drawable/ic_launcher_foreground.xml + ic_launcher_background.xml):
 * lime precision reticle with white corner brackets on a twilight radial gradient. 108-unit adaptive canvas.
 */
export const AppIcon: React.FC<{size: number; radius?: number; glow?: boolean}> = ({size, radius = size * 0.23, glow = true}) => (
	<div
		style={{
			width: size,
			height: size,
			borderRadius: radius,
			overflow: 'hidden',
			flexShrink: 0,
			background: 'radial-gradient(circle at 35% 30%, #3A2F6B 0%, #1C1E36 55%, #111324 100%)',
			boxShadow: glow ? `0 ${size * 0.08}px ${size * 0.25}px rgba(4,5,14,0.6), 0 0 ${size * 0.18}px rgba(221,248,61,0.25), inset 0 0 0 ${Math.max(1, size * 0.008)}px rgba(255,255,255,0.18)` : undefined,
		}}
	>
		<svg width={size} height={size} viewBox="21 21 66 66">
			<circle cx="54" cy="54" r="24" stroke={C.lime} strokeWidth="3.5" fill="none" />
			<path d="M36,42 L36,36 L42,36 M66,36 L72,36 L72,42 M72,66 L72,72 L66,72 M42,72 L36,72 L36,66" stroke={C.head} strokeWidth="2.5" strokeLinecap="round" fill="none" />
			<path d="M54,40 L54,48 M54,60 L54,68 M40,54 L48,54 M60,54 L68,54" stroke={C.lime} strokeWidth="2.5" strokeLinecap="round" />
			<circle cx="54" cy="54" r="3.5" fill={C.lime} />
		</svg>
	</div>
);
