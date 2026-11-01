import React from 'react';
import {useCurrentFrame, useVideoConfig} from 'remotion';
import {easeOut, prog} from '../lib';

/** Word-by-word mask reveal: each word slides up out of a clip with a slight tilt (~0.8 s, 0.09 s stagger). */
export const Reveal: React.FC<{
	text: string;
	at: number;
	size: number;
	weight?: number;
	italic?: boolean;
	color?: string;
	accent?: string;
	family: string;
	stagger?: number;
	align?: 'left' | 'center';
	lineHeight?: number;
}> = ({text, at, size, weight = 800, italic = true, color = '#F3F4FB', accent = '#DDF83D', family, stagger = 0.09, align = 'left', lineHeight = 1.06}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const t = frame / fps;
	// "*word*" marks the single lime accent.
	const words = text.split(' ');
	return (
		<div
			style={{
				display: 'flex',
				flexWrap: 'wrap',
				justifyContent: align === 'center' ? 'center' : 'flex-start',
				columnGap: size * 0.26,
				fontFamily: family,
				fontWeight: weight,
				fontStyle: italic ? 'italic' : 'normal',
				fontSize: size,
				lineHeight,
				letterSpacing: '-0.025em',
			}}
		>
			{words.map((w, i) => {
				const isAccent = w.startsWith('*');
				const clean = w.replace(/\*/g, '');
				const p = prog(t, at + i * stagger, at + i * stagger + 0.8, easeOut);
				return (
					<span key={i} style={{display: 'inline-block', overflow: 'hidden', paddingBottom: size * 0.12, paddingRight: size * 0.06, marginBottom: -size * 0.12}}>
						<span
							style={{
								display: 'inline-block',
								color: isAccent ? accent : color,
								translate: `0 ${(1 - p) * 110}%`,
								rotate: `${(1 - p) * 6}deg`,
								textShadow: '0 6px 36px rgba(5,6,18,0.55)',
							}}
						>
							{clean}
						</span>
					</span>
				);
			})}
		</div>
	);
};
