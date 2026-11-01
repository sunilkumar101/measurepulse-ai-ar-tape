import React from 'react';
import {C, F} from '../lib';

type Pt = {x: number; y: number};

/**
 * Electric-lime laser between two anchors, drawn in the coordinate space of its parent
 * (screenshot space inside <Phone>, or canvas px). `draw` 0..1 extends the beam from A to B.
 */
export const Laser: React.FC<{
	a: Pt;
	b: Pt;
	draw: number;
	anchorA: number;
	anchorB: number;
	label?: string;
	sub?: string;
	labelIn?: number;
	flash?: number;
	scale?: number;
}> = ({a, b, draw, anchorA, anchorB, label, sub, labelIn = 0, flash = 0, scale = 1}) => {
	const ex = a.x + (b.x - a.x) * draw;
	const ey = a.y + (b.y - a.y) * draw;
	const mx = (a.x + b.x) / 2;
	const my = (a.y + b.y) / 2;
	const angle = (Math.atan2(b.y - a.y, b.x - a.x) * 180) / Math.PI;
	const s = scale;
	const anchor = (p: Pt, v: number, key: string) =>
		v > 0 ? (
			<g key={key} opacity={Math.min(1, v * 1.4)}>
				<circle cx={p.x} cy={p.y} r={26 * s * v} fill="rgba(221,248,61,0.18)" />
				<circle cx={p.x} cy={p.y} r={14 * s} fill={C.lime} transform={`translate(${p.x} ${p.y}) scale(${v}) translate(${-p.x} ${-p.y})`} />
				<circle cx={p.x} cy={p.y} r={5.5 * s} fill={C.ink} />
			</g>
		) : null;
	return (
		<>
			<svg style={{position: 'absolute', left: 0, top: 0, overflow: 'visible'}} width="1" height="1">
				<defs>
					<filter id="laserGlow" x="-50%" y="-50%" width="200%" height="200%">
						<feGaussianBlur stdDeviation={6 * s} />
					</filter>
				</defs>
				{draw > 0 ? (
					<>
						<line x1={a.x} y1={a.y} x2={ex} y2={ey} stroke="rgba(221,248,61,0.65)" strokeWidth={14 * s} strokeLinecap="round" filter="url(#laserGlow)" />
						<line x1={a.x} y1={a.y} x2={ex} y2={ey} stroke={C.lime} strokeWidth={4 * s} strokeLinecap="round" />
						<line x1={a.x} y1={a.y} x2={ex} y2={ey} stroke="#FBFFE0" strokeWidth={1.4 * s} strokeLinecap="round" />
					</>
				) : null}
				{anchor(a, anchorA, 'a')}
				{anchor(b, anchorB, 'b')}
				{flash > 0 ? (
					<circle cx={b.x} cy={b.y} r={20 * s + flash * 90 * s} fill="none" stroke={C.lime} strokeWidth={4 * s * (1 - flash)} opacity={1 - flash} />
				) : null}
			</svg>
			{label && labelIn > 0 ? (
				<div
					style={{
						position: 'absolute',
						left: mx,
						top: my - 64 * s,
						translate: '-50% -50%',
						rotate: `${angle}deg`,
						scale: String(0.6 + 0.4 * labelIn),
						opacity: Math.min(1, labelIn * 1.5),
						display: 'flex',
						alignItems: 'baseline',
						gap: 12 * s,
						padding: `${12 * s}px ${22 * s}px`,
						borderRadius: 999,
						background: C.lime,
						boxShadow: `0 0 ${26 * s}px ${C.limeGlow}`,
						whiteSpace: 'nowrap',
					}}
				>
					<span style={{fontFamily: F.mono, fontWeight: 700, fontSize: 40 * s, color: C.ink}}>{label}</span>
					{sub ? <span style={{fontFamily: F.mono, fontWeight: 600, fontSize: 24 * s, color: C.ink, opacity: 0.8}}>{sub}</span> : null}
				</div>
			) : null}
		</>
	);
};
