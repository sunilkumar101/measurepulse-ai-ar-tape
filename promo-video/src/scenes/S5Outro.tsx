import React from 'react';
import {AbsoluteFill, Img, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {AppIcon} from '../components/AppIcon';
import {DotGrid, PhotoBackground} from '../components/Background';
import {LimePill, SpatialLabel} from '../components/Glass';
import {Reveal} from '../components/Reveal';
import {C, F, HEAVY, POP, prog, sp, TL} from '../lib';

const B = TL.s5;

const Widget: React.FC<{name: string; w: number; h: number; scale: number; i: number; tilt: number}> = ({name, w, h, scale, i, tilt}) => {
	const frame = useCurrentFrame();
	const {fps} = useVideoConfig();
	const t = frame / fps;
	const s = sp(frame, fps, B.widgets + i * B.widgetStagger, HEAVY);
	const float = Math.sin(t * 1.6 + i * 1.3) * 6;
	return (
		<div style={{perspective: 1400, opacity: Math.min(1, s * 1.4)}}>
			<Img
				src={staticFile(`widgets/${name}.png`)}
				style={{
					width: w * scale,
					height: h * scale,
					display: 'block',
					transform: `translateY(${(1 - s) * 120 + float}px) rotateX(${(1 - s) * 30 + 6}deg) rotateY(${tilt * (1 - s * 0.6)}deg)`,
					filter: 'drop-shadow(0 30px 50px rgba(4,5,14,0.6)) drop-shadow(0 0 30px rgba(221,248,61,0.12))',
				}}
			/>
		</div>
	);
};

/** 15–18 s · The three real frosted glass home-screen widgets, then the call to action (held ≥ 2 s). */
export const S5Outro: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps, width, height} = useVideoConfig();
	const L = width > height;
	const t = frame / fps;
	const title = sp(frame, fps, B.title, HEAVY);
	const cta = sp(frame, fps, B.cta, POP);
	const breathe = 0.45 + 0.25 * Math.sin(t * Math.PI * 2 * 0.8);
	const k = L ? 1.0 : 1.18;

	const widgets = (
		<div style={{display: 'flex', flexDirection: 'column', gap: 26 * k, alignItems: 'flex-start'}}>
			<Widget name="hub" w={622} h={348} scale={k} i={0} tilt={-14} />
			<div style={{display: 'flex', gap: 26 * k, alignItems: 'flex-start'}}>
				<Widget name="converter" w={294} h={348} scale={k} i={1} tilt={-10} />
				<div style={{display: 'flex', flexDirection: 'column', gap: 18 * k, paddingTop: 10}}>
					<SpatialLabel size={L ? 20 : 24} color={C.lime} style={{opacity: prog(t, B.widgets + 0.5, B.widgets + 0.9)}}>
						3 glass widgets
					</SpatialLabel>
					<div style={{fontFamily: F.sans, fontWeight: 600, fontSize: L ? 28 : 34, color: C.ice, width: 290 * k, lineHeight: 1.3, opacity: prog(t, B.widgets + 0.55, B.widgets + 1.0)}}>
						Your last room and every tool, one tap from your home screen.
					</div>
				</div>
			</div>
			<Widget name="dock" w={622} h={156} scale={k} i={2} tilt={-8} />
		</div>
	);

	const brand = (
		<div style={{display: 'flex', flexDirection: 'column', alignItems: L ? 'flex-start' : 'center', gap: 26, opacity: title, translate: `0 ${(1 - title) * 50}px`}}>
			<div style={{display: 'flex', alignItems: 'center', gap: 28}}>
				<AppIcon size={L ? 140 : 150} />
				<div style={{display: 'flex', flexDirection: 'column', gap: 6}}>
					<div style={{fontFamily: F.sans, fontWeight: 800, fontSize: L ? 84 : 88, color: C.head, letterSpacing: '-0.03em', lineHeight: 1}}>
						MeasurePulse <span style={{color: C.lime}}>AI</span>
					</div>
					<SpatialLabel size={L ? 20 : 22}>AR tape · Blueprint · Caliper · Level</SpatialLabel>
				</div>
			</div>
			<Reveal text="Sub-MM Laser *Precision*" at={B.title + 0.1} size={L ? 80 : 84} family={F.sans} align={L ? 'left' : 'center'} />
			<div style={{scale: String(0.7 + 0.3 * cta), opacity: cta, marginTop: 10}}>
				<LimePill size={L ? 50 : 54} glow={breathe}>
					<svg width={L ? 40 : 46} height={L ? 40 : 46} viewBox="0 0 24 24" fill="none" stroke={C.ink} strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round">
						<path d="M12 4v11M7 10l5 5 5-5M5 20h14" />
					</svg>
					Download Now
				</LimePill>
			</div>
		</div>
	);

	return (
		<AbsoluteFill style={{opacity: prog(t, 0, 0.45)}}>
			<PhotoBackground src="bg/bg_villa_pool.jpg" blur={10} dim={0.55} scale={1.12 + t * 0.012} />
			<DotGrid opacity={0.08} />
			{L ? (
				<AbsoluteFill style={{flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: '0 150px 0 140px'}}>
					{widgets}
					{brand}
				</AbsoluteFill>
			) : (
				<AbsoluteFill style={{flexDirection: 'column', alignItems: 'center', justifyContent: 'space-between', padding: '170px 0 190px'}}>
					{widgets}
					{brand}
				</AbsoluteFill>
			)}
		</AbsoluteFill>
	);
};
