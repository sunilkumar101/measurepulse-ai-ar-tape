import React from 'react';
import {AbsoluteFill, useCurrentFrame, useVideoConfig} from 'remotion';
import {PhotoBackground} from '../components/Background';
import {Box3D} from '../components/Box3D';
import {Glass, GlassPill, LimePill, SpatialLabel} from '../components/Glass';
import {Phone} from '../components/Phone';
import {C, easeIn, easeInOut, easeOut, F, HEAVY, lerp, NOTCH, POP, prog, sp, TL} from '../lib';

const B = TL.s3;

// "Master Suite & Terrace": L-shaped room, metres. Area 24.82 m², perimeter 21.30 m.
const ROOM: Array<[number, number]> = [
	[0, 0],
	[5.2, 0],
	[5.2, 3.25],
	[3.6, 3.25],
	[3.6, 5.45],
	[0, 5.45],
];
const AREA = 24.8;
const SQFT = 266.9;

const FloorPlan: React.FC<{k: number; t: number}> = ({k, t}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const pad = 46;
	const W = 5.2 * k + pad * 2;
	const H = 5.45 * k + pad * 2;
	const pts = ROOM.map(([x, y]) => [pad + x * k, pad + y * k] as const);
	const d = pts.map((p, i) => `${i ? 'L' : 'M'}${p[0]},${p[1]}`).join(' ') + ' Z';
	const drawn = prog(t, B.plan, B.plan + 0.9, easeInOut);
	const fill = prog(t, B.plan + 0.7, B.plan + 1.2);
	const labelsIn = prog(t, B.plan + 0.8, B.plan + 1.3);
	const dims: Array<[number, number, string, 'h' | 'v']> = [
		[2.6, 0, '5.20 m', 'h'],
		[5.2, 1.62, '3.25 m', 'v'],
		[4.4, 3.25, '1.60 m', 'h'],
		[3.6, 4.35, '2.20 m', 'v'],
		[1.8, 5.45, '3.60 m', 'h'],
		[0, 2.72, '5.45 m', 'v'],
	];
	return (
		<div style={{position: 'relative', width: W, height: H, flexShrink: 0}}>
			<svg width={W} height={H} style={{position: 'absolute', inset: 0}}>
				<defs>
					<pattern id="grid" width={k / 2} height={k / 2} patternUnits="userSpaceOnUse">
						<path d={`M ${k / 2} 0 L 0 0 0 ${k / 2}`} fill="none" stroke="rgba(158,159,185,0.14)" strokeWidth="1" />
					</pattern>
				</defs>
				<rect width={W} height={H} rx="22" fill="rgba(10,12,28,0.55)" />
				<rect width={W} height={H} rx="22" fill="url(#grid)" />
				<path d={d} fill={`rgba(221,248,61,${0.09 * fill})`} stroke="none" />
				<path d={d} fill="none" stroke="rgba(221,248,61,0.35)" strokeWidth="14" pathLength={1} strokeDasharray="1" strokeDashoffset={1 - drawn} strokeLinejoin="round" style={{filter: 'blur(6px)'}} />
				<path d={d} fill="none" stroke={C.head} strokeWidth="6" pathLength={1} strokeDasharray="1" strokeDashoffset={1 - drawn} strokeLinejoin="round" />
				{pts.map((p, i) => {
					const s = sp(frame, fps, B.plan + 0.12 + i * 0.12, POP);
					return (
						<g key={i}>
							<circle cx={p[0]} cy={p[1]} r={15 * s} fill={C.lime} />
							<circle cx={p[0]} cy={p[1]} r={5 * s} fill={C.ink} />
						</g>
					);
				})}
				<text x={pad + 1.8 * k} y={pad + 2.0 * k} fill={C.lime} opacity={fill} fontFamily={F.mono} fontWeight={700} fontSize={k * 0.32} textAnchor="middle">
					{AREA.toFixed(1)} m²
				</text>
			</svg>
			{dims.map(([x, y, txt, o], i) => (
				<div
					key={i}
					style={{
						position: 'absolute',
						left: pad + x * k,
						top: pad + y * k,
						translate: '-50% -50%',
						rotate: o === 'v' ? '90deg' : '0deg',
						opacity: labelsIn,
						padding: '5px 12px',
						borderRadius: 999,
						background: C.ink,
						border: `1.5px solid rgba(221,248,61,0.6)`,
						fontFamily: F.mono,
						fontWeight: 600,
						fontSize: k * 0.2,
						color: C.lime,
						whiteSpace: 'nowrap',
					}}
				>
					{txt}
				</div>
			))}
		</div>
	);
};

const BoxFit: React.FC<{t: number; w: number}> = ({t, w}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const m = w / 2.3; // px per metre inside this panel
	const gap = 1.82 * m;
	const tvW = 1.68 * m;
	const tvH = 0.97 * m;
	const appear = prog(t, B.boxIn, B.boxIn + 0.45);
	const snap = sp(frame, fps, B.boxSnap, NOTCH);
	const fits = sp(frame, fps, B.fits, POP);
	const bob = Math.sin(t * 3.2) * 10 * (1 - snap);
	const tx = lerp(w * 0.42, 0, snap);
	const ty = lerp(-tvH * 0.75, 0, snap) + bob;
	const ry = lerp(42, -14, snap);
	const rx = lerp(-24, -8, snap);
	const wallH = tvH * 1.65;
	return (
		<div style={{display: 'flex', flexDirection: 'column', gap: 18, width: w}}>
			<SpatialLabel size={18} color={C.lime}>
				3D Box Fit · 75" TV
			</SpatialLabel>
			<div style={{position: 'relative', width: w, height: wallH + 40, perspective: 900}}>
				{/* wall niche */}
				<div style={{position: 'absolute', left: (w - gap) / 2, top: 20, width: gap, height: wallH, borderLeft: `3px dashed rgba(243,244,251,0.7)`, borderRight: `3px dashed rgba(243,244,251,0.7)`, background: 'rgba(243,244,251,0.04)'}} />
				<div style={{position: 'absolute', left: w / 2, top: wallH + 26, translate: '-50% 0', fontFamily: F.mono, fontWeight: 600, fontSize: 18, color: C.ice, opacity: 0.85, whiteSpace: 'nowrap'}}>
					⟵ 1.82 m wall ⟶
				</div>
				<div style={{position: 'absolute', left: w / 2, top: 20 + wallH / 2, opacity: appear}}>
					<Box3D w={tvW} h={tvH} d={m * 0.12} glow={0.3 + 0.7 * snap} transform={`translate3d(${tx}px, ${ty}px, 0) rotateX(${rx}deg) rotateY(${ry}deg)`} />
				</div>
			</div>
			<div style={{scale: String(0.6 + 0.4 * fits), opacity: fits, transformOrigin: 'left center'}}>
				<LimePill size={w > 400 ? 26 : 21} mono>
					✓ Fits · +14 cm clearance
				</LimePill>
			</div>
		</div>
	);
};

/** 7–11 s · The frosted glass blueprint sheet slides up over the scan; the plan draws itself; a TV box snaps in. */
export const S3Blueprint: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps, width, height} = useVideoConfig();
	const L = width > height;
	const t = frame / fps;

	const phoneIn = sp(frame, fps, B.phoneIn, HEAVY);
	const sheet = sp(frame, fps, B.sheetIn, HEAVY);
	const exit = prog(t, B.exit, B.exit + 0.4, easeIn);
	const area = prog(t, B.area, B.area + 0.9, easeOut);

	const k = L ? 76 : 70;
	return (
		<AbsoluteFill style={{opacity: (1 - exit) * prog(t, 0, 0.45)}}>
			<PhotoBackground src="bg/bg_city_lounge.jpg" blur={16} dim={0.58} scale={1.18 - t * 0.012} x={-t * 8} />
			{/* Parallax: the phone moves slower than the sheet. */}
			<AbsoluteFill
				style={{
					alignItems: L ? 'flex-start' : 'center',
					justifyContent: L ? 'center' : 'flex-start',
					paddingLeft: L ? 170 : 0,
					paddingTop: L ? 90 : 200,
					translate: `0 ${(1 - phoneIn) * 500 - exit * 120}px`,
					opacity: phoneIn,
				}}
			>
				<Phone src="screens/raw/04_room_blueprint.png" height={L ? 820 : 900} rotateY={L ? 16 : 0} rotateX={L ? 4 : 10} glow={0.2} />
			</AbsoluteFill>

			<div
				style={{
					position: 'absolute',
					left: L ? 740 : 50,
					top: L ? 110 : 900,
					width: L ? 1050 : 980,
					translate: `0 ${(1 - sheet) * (L ? 1000 : 1200) + exit * -60}px`,
				}}
			>
				<Glass radius={48} tint="rgba(18,21,42,0.80)">
					<div style={{padding: L ? '20px 44px 40px' : '20px 44px 44px', display: 'flex', flexDirection: 'column', gap: 22}}>
						<div style={{alignSelf: 'center', width: 76, height: 8, borderRadius: 8, background: 'rgba(255,255,255,0.28)'}} />
						<div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end'}}>
							<div style={{display: 'flex', flexDirection: 'column', gap: 10}}>
								<SpatialLabel size={20} color={C.lime}>
									02 · Room Blueprint
								</SpatialLabel>
								<div style={{fontFamily: F.sans, fontWeight: 800, fontSize: L ? 54 : 56, color: C.head, letterSpacing: '-0.02em', lineHeight: 1.04}}>
									Master Suite{L ? ' & Terrace' : ''}
								</div>
							</div>
							<div style={{textAlign: 'right'}}>
								<div style={{fontFamily: F.mono, fontWeight: 700, fontSize: L ? 76 : 80, color: C.lime, lineHeight: 1, textShadow: `0 0 30px ${C.limeGlow}`}}>
									{(AREA * area).toFixed(1)}
									<span style={{fontSize: L ? 40 : 42}}> m²</span>
								</div>
								<div style={{fontFamily: F.mono, fontWeight: 500, fontSize: 24, color: C.muted, marginTop: 6}}>/ {(SQFT * area).toFixed(1)} sq.ft</div>
							</div>
						</div>
						<div style={{display: 'flex', gap: 12, opacity: prog(t, B.area, B.area + 0.4)}}>
							<GlassPill size={22}>6 Walls</GlassPill>
							<GlassPill size={22}>Ceiling 2.85 m</GlassPill>
							<GlassPill size={22}>Perimeter 21.30 m</GlassPill>
						</div>
						<div style={{display: 'flex', alignItems: 'center', gap: L ? 44 : 30}}>
							<FloorPlan k={k} t={t} />
							<BoxFit t={t} w={L ? 420 : 380} />
						</div>
					</div>
				</Glass>
			</div>
		</AbsoluteFill>
	);
};
