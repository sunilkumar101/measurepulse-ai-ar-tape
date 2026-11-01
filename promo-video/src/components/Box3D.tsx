import React from 'react';
import {C} from '../lib';

/** Translucent lime wireframe box (the AR "3D Box Fit"). Origin is its centre. */
export const Box3D: React.FC<{w: number; h: number; d: number; transform: string; glow?: number; label?: string}> = ({w, h, d, transform, glow = 0.4}) => {
	const face = (fw: number, fh: number, tf: string, fill: string, key: string) => (
		<div
			key={key}
			style={{
				position: 'absolute',
				left: -fw / 2,
				top: -fh / 2,
				width: fw,
				height: fh,
				transform: tf,
				background: fill,
				border: `2px solid ${C.lime}`,
				boxSizing: 'border-box',
				boxShadow: `0 0 14px rgba(221,248,61,${glow * 0.6}) inset`,
			}}
		/>
	);
	return (
		<div style={{position: 'absolute', left: 0, top: 0, transformStyle: 'preserve-3d', transform}}>
			{face(w, h, `translateZ(${d / 2}px)`, 'linear-gradient(135deg, rgba(221,248,61,0.20), rgba(17,19,36,0.55) 60%, rgba(221,248,61,0.10))', 'front')}
			{face(w, h, `rotateY(180deg) translateZ(${d / 2}px)`, 'rgba(221,248,61,0.06)', 'back')}
			{face(d, h, `rotateY(90deg) translateZ(${w / 2}px)`, 'rgba(221,248,61,0.14)', 'right')}
			{face(d, h, `rotateY(-90deg) translateZ(${w / 2}px)`, 'rgba(221,248,61,0.14)', 'left')}
			{face(w, d, `rotateX(90deg) translateZ(${h / 2}px)`, 'rgba(221,248,61,0.18)', 'top')}
			{face(w, d, `rotateX(-90deg) translateZ(${h / 2}px)`, 'rgba(221,248,61,0.08)', 'bottom')}
		</div>
	);
};
