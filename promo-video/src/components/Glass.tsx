import React from 'react';
import {C, F} from '../lib';

/** Smoked frosted glass card: rgba(18,21,42,.68) + backdrop blur + 1px specular rim (the app's SmokedFrostedGlassCard). */
export const Glass: React.FC<{
	children?: React.ReactNode;
	radius?: number;
	style?: React.CSSProperties;
	tint?: string;
	blur?: number;
}> = ({children, radius = 28, style, tint = C.glass, blur = 26}) => (
	<div
		style={{
			position: 'relative',
			borderRadius: radius,
			background: tint,
			backdropFilter: `blur(${blur}px) saturate(1.25)`,
			border: `1.5px solid ${C.rim}`,
			boxShadow: '0 30px 80px rgba(5, 6, 18, 0.55), inset 0 1px 0 rgba(255,255,255,0.18)',
			overflow: 'hidden',
			...style,
		}}
	>
		{children}
	</div>
);

/** Electric lime pill — text on lime is always Deep Midnight #111322, bold. */
export const LimePill: React.FC<{children: React.ReactNode; size?: number; mono?: boolean; glow?: number; style?: React.CSSProperties}> = ({
	children,
	size = 28,
	mono = false,
	glow = 0.45,
	style,
}) => (
	<div
		style={{
			display: 'inline-flex',
			alignItems: 'center',
			gap: size * 0.45,
			padding: `${size * 0.42}px ${size * 0.85}px`,
			borderRadius: 999,
			background: C.lime,
			color: C.ink,
			fontFamily: mono ? F.mono : F.sans,
			fontWeight: mono ? 700 : 800,
			fontSize: size,
			letterSpacing: mono ? '0.01em' : '-0.01em',
			whiteSpace: 'nowrap',
			boxShadow: glow > 0 ? `0 0 ${size * 1.1}px rgba(221,248,61,${glow})` : undefined,
			...style,
		}}
	>
		{children}
	</div>
);

export const GlassPill: React.FC<{children: React.ReactNode; size?: number; style?: React.CSSProperties}> = ({children, size = 26, style}) => (
	<div
		style={{
			display: 'inline-flex',
			alignItems: 'center',
			gap: size * 0.5,
			padding: `${size * 0.45}px ${size * 0.9}px`,
			borderRadius: 999,
			background: 'rgba(18,21,42,0.72)',
			backdropFilter: 'blur(20px)',
			border: `1.5px solid ${C.rim}`,
			color: C.head,
			fontFamily: F.sans,
			fontWeight: 600,
			fontSize: size,
			whiteSpace: 'nowrap',
			...style,
		}}
	>
		{children}
	</div>
);

/** Uppercase spatial label: mono, +0.2em tracking, muted. Decoration, not message. */
export const SpatialLabel: React.FC<{children: React.ReactNode; size?: number; color?: string; style?: React.CSSProperties}> = ({
	children,
	size = 20,
	color = C.muted,
	style,
}) => (
	<div style={{fontFamily: F.mono, fontWeight: 600, fontSize: size, letterSpacing: '0.2em', textTransform: 'uppercase', color, ...style}}>
		{children}
	</div>
);

/** Display headline with one lime accent (`*word*`). */
export const Headline: React.FC<{text: string; size: number; italic?: boolean; align?: 'left' | 'center'; style?: React.CSSProperties}> = ({
	text,
	size,
	italic = true,
	align = 'left',
	style,
}) => {
	const parts = text.split(/(\*[^*]+\*)/g).filter(Boolean);
	return (
		<div
			style={{
				fontFamily: F.sans,
				fontWeight: 800,
				fontStyle: italic ? 'italic' : 'normal',
				fontSize: size,
				lineHeight: 1.02,
				letterSpacing: '-0.025em',
				color: C.head,
				textAlign: align,
				textShadow: '0 6px 40px rgba(5,6,18,0.55)',
				...style,
			}}
		>
			{parts.map((p, i) =>
				p.startsWith('*') ? (
					// The accent phrase never breaks across lines (no orphaned accent word).
					<span key={i} style={{color: C.lime, whiteSpace: 'nowrap'}}>
						{p.slice(1, -1)}
					</span>
				) : (
					<span key={i}>{p}</span>
				),
			)}
		</div>
	);
};
