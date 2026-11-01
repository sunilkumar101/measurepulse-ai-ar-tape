import React from 'react';
import {AbsoluteFill, useCurrentFrame, useVideoConfig} from 'remotion';
import {DotGrid, PhotoBackground, TwilightGradient} from '../components/Background';
import {Glass, LimePill, SpatialLabel} from '../components/Glass';
import {Orb} from '../components/Orb';
import {Reveal} from '../components/Reveal';
import {C, easeIn, easeOut, F, HEAVY, lerp, overshoot, prog, SETTLE, sp, TL} from '../lib';

const B = TL.s4;

const Icon: React.FC<{kind: 'tiles' | 'paint' | 'ac' | 'tv'; size: number}> = ({kind, size}) => (
	<svg width={size} height={size} viewBox="0 0 48 48" fill="none" stroke={C.ink} strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round">
		{kind === 'tiles' ? (
			<>
				<rect x="8" y="8" width="14" height="14" rx="2" />
				<rect x="26" y="8" width="14" height="14" rx="2" />
				<rect x="8" y="26" width="14" height="14" rx="2" />
				<rect x="26" y="26" width="14" height="14" rx="2" />
			</>
		) : kind === 'paint' ? (
			<>
				<rect x="8" y="8" width="26" height="12" rx="3" />
				<path d="M34 14h6v10H24v6" />
				<rect x="20" y="30" width="8" height="12" rx="2" />
			</>
		) : kind === 'ac' ? (
			<path d="M24 6v36M8.4 15l31.2 18M8.4 33l31.2-18M19 9l5 5 5-5M19 39l5-5 5 5" />
		) : (
			<>
				<rect x="6" y="9" width="36" height="24" rx="3" />
				<path d="M17 41h14M24 33v8" />
			</>
		)}
	</svg>
);

const StatCard: React.FC<{i: number; kind: 'tiles' | 'paint' | 'ac' | 'tv'; to: number; dec: number; unit: string; label: string; sub: string; L: boolean}> = ({i, kind, to, dec, unit, label, sub, L}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const t = frame / fps;
	const at = B.cards + i * B.cardStagger;
	const s = sp(frame, fps, at, SETTLE);
	const count = prog(t, at + 0.1, at + 0.9, easeOut);
	return (
		<div style={{opacity: Math.min(1, s * 1.3), translate: `0 ${(1 - s) * 90}px`, rotate: `${(1 - s) * -4}deg`}}>
			<Glass radius={30} style={{width: L ? 404 : 440, height: L ? 244 : 250}}>
				<div style={{padding: '26px 28px', display: 'flex', flexDirection: 'column', height: '100%', boxSizing: 'border-box', justifyContent: 'space-between'}}>
					<div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
						<div style={{width: 62, height: 62, borderRadius: 18, background: C.lime, display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: `0 0 22px ${C.limeGlow}`}}>
							<Icon kind={kind} size={38} />
						</div>
						<SpatialLabel size={17}>{label}</SpatialLabel>
					</div>
					<div>
						<div style={{fontFamily: F.mono, fontWeight: 700, fontSize: L ? 66 : 70, color: C.head, lineHeight: 1}}>
							{(to * count).toFixed(dec)}
							<span style={{fontSize: L ? 34 : 36, color: C.lime}}> {unit}</span>
						</div>
						<div style={{fontFamily: F.sans, fontWeight: 500, fontSize: 23, color: C.ice, marginTop: 8}}>{sub}</div>
					</div>
				</div>
			</Glass>
		</div>
	);
};

const Caliper: React.FC<{w: number; t: number}> = ({w, t}) => {
	const pxPerMm = (w - 120) / 32;
	const mm = lerp(8, 24.25, prog(t, B.jaws, B.jaws + 0.85, overshoot));
	const x0 = 60;
	const jaw = x0 + mm * pxPerMm;
	return (
		<div style={{display: 'flex', flexDirection: 'column', gap: 16}}>
			<svg width={w} height={150}>
				<rect x="20" y="34" width={w - 40} height="80" rx="16" fill="rgba(10,12,28,0.6)" stroke="rgba(255,255,255,0.18)" />
				{new Array(33).fill(0).map((_, i) => (
					<line key={i} x1={x0 + i * pxPerMm} y1="34" x2={x0 + i * pxPerMm} y2={34 + (i % 10 === 0 ? 34 : i % 5 === 0 ? 24 : 14)} stroke={i % 5 === 0 ? C.lime : 'rgba(243,244,251,0.55)'} strokeWidth={i % 5 === 0 ? 2.4 : 1.4} />
				))}
				{[0, 10, 20, 30].map((v) => (
					<text key={v} x={x0 + v * pxPerMm} y="98" fill={C.ice} fontFamily={F.mono} fontSize="18" textAnchor="middle">
						{v / 10}
					</text>
				))}
				<rect x={x0 - 4} y="10" width="8" height="128" rx="4" fill={C.lime} />
				<rect x={jaw - 4} y="10" width="8" height="128" rx="4" fill={C.lime} style={{filter: `drop-shadow(0 0 10px ${C.limeGlow})`}} />
				<rect x={x0} y="122" width={jaw - x0} height="6" fill="rgba(221,248,61,0.35)" />
			</svg>
			<div style={{display: 'flex', alignItems: 'baseline', gap: 16}}>
				<span style={{fontFamily: F.mono, fontWeight: 700, fontSize: 64, color: C.lime, textShadow: `0 0 24px ${C.limeGlow}`}}>{mm.toFixed(2)}</span>
				<span style={{fontFamily: F.mono, fontWeight: 600, fontSize: 30, color: C.lime}}>mm</span>
				<span style={{fontFamily: F.mono, fontWeight: 500, fontSize: 24, color: C.muted, marginLeft: 8}}>{(mm / 25.4).toFixed(3)} in</span>
			</div>
		</div>
	);
};

const Level: React.FC<{size: number}> = ({size}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const settle = sp(frame, fps, B.bubble, {damping: 20, stiffness: 140, mass: 0.9});
	const ox = lerp(size * 0.24, 0, settle);
	const oy = lerp(-size * 0.17, 0, settle);
	const deg = (Math.hypot(ox, oy) / (size / 2)) * 10;
	const ok = deg < 0.05;
	const r = size / 2;
	return (
		<div style={{display: 'flex', alignItems: 'center', gap: 28}}>
			<div style={{position: 'relative', width: size, height: size}}>
				<svg width={size} height={size} style={{position: 'absolute', inset: 0}}>
					<circle cx={r} cy={r} r={r - 2} fill="rgba(10,12,28,0.6)" stroke="rgba(255,255,255,0.3)" strokeWidth="2" />
					<circle cx={r} cy={r} r={r * 0.62} fill="none" stroke="rgba(255,255,255,0.22)" strokeDasharray="6 6" />
					<circle cx={r} cy={r} r={r * 0.3} fill="none" stroke="rgba(255,255,255,0.22)" strokeDasharray="6 6" />
					<line x1={8} y1={r} x2={size - 8} y2={r} stroke="rgba(255,255,255,0.18)" />
					<line x1={r} y1={8} x2={r} y2={size - 8} stroke="rgba(255,255,255,0.18)" />
				</svg>
				<div
					style={{
						position: 'absolute',
						left: r + ox,
						top: r + oy,
						translate: '-50% -50%',
						width: size * 0.32,
						height: size * 0.32,
						borderRadius: '50%',
						background: 'radial-gradient(circle at 35% 30%, #F2FF9E, #DDF83D 55%, #9DB51C)',
						boxShadow: ok ? `0 0 ${size * 0.18}px rgba(221,248,61,0.8)` : `0 0 ${size * 0.06}px rgba(221,248,61,0.4)`,
					}}
				/>
			</div>
			<div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
				<span style={{fontFamily: F.mono, fontWeight: 700, fontSize: 64, color: C.head}}>{deg.toFixed(1)}°</span>
				<div style={{opacity: ok ? 1 : 0.0}}>
					<LimePill size={22} mono glow={0.6}>
						✓ LEVEL
					</LimePill>
				</div>
			</div>
		</div>
	);
};

/** 11–15 s · "Help With AI" cards cascade in, then the caliper and bullseye level settle. */
export const S4Advisor: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps, width, height} = useVideoConfig();
	const L = width > height;
	const t = frame / fps;
	const exit = prog(t, B.exit, B.exit + 0.4, easeIn);
	const header = sp(frame, fps, B.header, HEAVY);
	const tools = sp(frame, fps, B.tools, HEAVY);
	const toolsP = sp(frame, fps, B.tools + 0.15, HEAVY);

	const cards = (
		<div style={{display: 'grid', gridTemplateColumns: L ? 'repeat(4, auto)' : 'repeat(2, auto)', gap: L ? 22 : 24, justifyContent: 'center'}}>
			<StatCard i={0} kind="tiles" to={76} dec={0} unit="" label="Tiles" sub="Floor tiles · +10 % cut" L={L} />
			<StatCard i={1} kind="paint" to={14.5} dec={1} unit="L" label="Paint" sub="Wall paint · 2 coats" L={L} />
			<StatCard i={2} kind="ac" to={1.5} dec={1} unit="Ton" label="AC" sub="18,000 BTU/h split" L={L} />
			<StatCard i={3} kind="tv" to={65} dec={0} unit={'"'} label="TV" sub="Ideal 4K screen size" L={L} />
		</div>
	);

	const toolW = L ? 832 : 940;
	return (
		<AbsoluteFill style={{opacity: (1 - exit) * prog(t, 0, 0.45), translate: `0 ${-exit * 40}px`}}>
			<TwilightGradient />
			<PhotoBackground src="bg/bg_ocean_room_scan.jpg" blur={28} dim={0.78} opacity={0.55} scale={1.2} />
			<DotGrid opacity={0.09} />
			<AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', paddingTop: L ? 40 : 60, gap: L ? 34 : 44}}>
				<div style={{display: 'flex', alignItems: 'center', gap: 26, opacity: header, translate: `0 ${(1 - header) * 30}px`}}>
					<Orb size={L ? 92 : 110} />
					<div style={{display: 'flex', flexDirection: 'column', gap: 8}}>
						<SpatialLabel size={L ? 20 : 24} color={C.lime}>
							03 · Spatial Room Advisor
						</SpatialLabel>
						<Reveal text="Help With *AI*" at={0.05} size={L ? 74 : 92} family={F.sans} />
					</div>
				</div>
				{cards}
				<div style={{display: 'flex', flexDirection: L ? 'row' : 'column', gap: L ? 24 : 26, marginTop: L ? 4 : 10}}>
					<div style={{opacity: tools, translate: `0 ${(1 - tools) * 120}px`}}>
						<Glass radius={34} style={{width: toolW}}>
							<div style={{padding: '24px 32px', display: 'flex', flexDirection: 'column', gap: 6}}>
								<SpatialLabel size={18}>Vernier caliper</SpatialLabel>
								<Caliper w={toolW - 64} t={t} />
							</div>
						</Glass>
					</div>
					<div style={{opacity: toolsP, translate: `0 ${(1 - toolsP) * 120}px`}}>
						<Glass radius={34} style={{width: toolW}}>
							<div style={{padding: '24px 32px', display: 'flex', flexDirection: 'column', gap: 10}}>
								<SpatialLabel size={18}>Bullseye spirit level</SpatialLabel>
								<Level size={L ? 210 : 240} />
							</div>
						</Glass>
					</div>
				</div>
			</AbsoluteFill>
		</AbsoluteFill>
	);
};
