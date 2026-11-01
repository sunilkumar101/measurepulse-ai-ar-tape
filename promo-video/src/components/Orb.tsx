import React from 'react';
import {useCurrentFrame, useVideoConfig} from 'remotion';
import {C, rand} from '../lib';

/** 3D iridescent violet-blue-pink AI orb (#6D3A9C / #B845ED, cyan hints) that breathes. */
export const Orb: React.FC<{size: number; energy?: number; dot?: boolean}> = ({size, energy = 1, dot = true}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const t = frame / fps;
	const pulse = 1 + 0.035 * energy * Math.sin(t * Math.PI * 2 * 0.9);
	const spin = (t * 38) % 360;
	return (
		<div style={{position: 'relative', width: size, height: size, flexShrink: 0, scale: String(pulse)}}>
			<div
				style={{
					position: 'absolute',
					inset: -size * 0.28,
					borderRadius: '50%',
					background: `radial-gradient(circle, rgba(184,69,237,${0.42 * energy}) 0%, rgba(109,58,156,0.18) 45%, transparent 70%)`,
				}}
			/>
			<div
				style={{
					position: 'absolute',
					inset: 0,
					borderRadius: '50%',
					background:
						'radial-gradient(circle at 34% 28%, #FFFFFF 0%, #F2E6FF 7%, #C79BFF 20%, #B845ED 42%, #6D3A9C 72%, #23173F 100%)',
					boxShadow: `0 0 ${size * 0.35}px rgba(184,69,237,0.55), inset -${size * 0.08}px -${size * 0.1}px ${size * 0.2}px rgba(20,10,50,0.6)`,
					overflow: 'hidden',
				}}
			>
				<div
					style={{
						position: 'absolute',
						inset: '-20%',
						background: 'conic-gradient(from 0deg, rgba(94,231,242,0.55), rgba(255,111,207,0.45), rgba(120,110,255,0.5), rgba(94,231,242,0.55))',
						mixBlendMode: 'screen',
						opacity: 0.55,
						rotate: `${spin}deg`,
						filter: `blur(${size * 0.08}px)`,
					}}
				/>
				<div
					style={{
						position: 'absolute',
						left: '18%',
						top: '12%',
						width: '34%',
						height: '22%',
						borderRadius: '50%',
						background: 'rgba(255,255,255,0.75)',
						filter: `blur(${size * 0.04}px)`,
					}}
				/>
			</div>
			{dot ? (
				<div
					style={{
						position: 'absolute',
						right: size * 0.04,
						bottom: size * 0.04,
						width: size * 0.22,
						height: size * 0.22,
						borderRadius: '50%',
						background: C.lime,
						border: `${size * 0.04}px solid ${C.ink}`,
						boxShadow: `0 0 ${size * 0.2}px ${C.limeGlow}`,
					}}
				/>
			) : null}
		</div>
	);
};

/** Lime voice waveform bars; `level` 0..1 drives amplitude (0 = idle dots). */
export const Waveform: React.FC<{bars?: number; height: number; level?: number; width?: number}> = ({bars = 11, height, level = 1, width = 6}) => {
	const frame = useCurrentFrame();
	return (
		<div style={{display: 'flex', alignItems: 'center', gap: width * 0.9, height}}>
			{new Array(bars).fill(0).map((_, i) => {
				// Two incommensurate sines + a held random per bar, re-picked on twos: reads as live speech.
				const a = Math.abs(Math.sin(frame * 0.21 + i * 0.9)) * 0.55 + Math.abs(Math.sin(frame * 0.083 + i * 2.3)) * 0.45;
				const jitter = rand(i * 31 + Math.floor(frame / 2)) * 0.35;
				const h = Math.max(width, height * Math.min(1, (a * 0.75 + jitter) * level));
				return (
					<div
						key={i}
						style={{
							width,
							height: h,
							borderRadius: width,
							background: i % 3 === 1 ? C.head : C.lime,
							boxShadow: `0 0 ${width * 1.5}px ${C.limeGlow}`,
						}}
					/>
				);
			})}
		</div>
	);
};
