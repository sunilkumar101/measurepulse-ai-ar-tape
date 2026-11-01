import React from 'react';
import {AbsoluteFill, useCurrentFrame, useVideoConfig} from 'remotion';
import {PhotoBackground} from '../components/Background';
import {Glass, LimePill, SpatialLabel} from '../components/Glass';
import {Laser} from '../components/Laser';
import {Orb, Waveform} from '../components/Orb';
import {Phone} from '../components/Phone';
import {Reveal} from '../components/Reveal';
import {C, easeIn, easeInOut, easeOut, F, HEAVY, lerp, POP, prog, sp, TL} from '../lib';

const B = TL.s2;

/** 3–7 s · 3D AR laser tape: a tilted phone zooms into the living room, the laser locks onto the sofa by voice. */
export const S2Tape: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps, width, height} = useVideoConfig();
	const L = width > height;
	const t = frame / fps;

	const enter = sp(frame, fps, B.phoneIn, HEAVY);
	const tilt = prog(t, B.phoneIn, B.tiltSettle, easeOut);
	const zoom = prog(t, B.tiltSettle, B.exit, easeInOut);
	const exit = prog(t, B.exit, B.exit + 0.4, easeIn);

	const aIn = sp(frame, fps, B.anchorA, POP);
	const draw = prog(t, B.anchorA + 0.05, B.anchorB, easeInOut);
	const bIn = sp(frame, fps, B.anchorB, POP);
	const flash = prog(t, B.anchorB, B.anchorB + 0.55, easeOut);
	const label = sp(frame, fps, B.label, POP);
	const say = sp(frame, fps, B.sayMark, HEAVY);
	const heard = prog(t, B.markHeard - 0.15, B.markHeard + 0.2);
	const locked = sp(frame, fps, B.anchorB + 0.1, POP);

	const phoneH = L ? 1000 : 1060;
	const phone = (
		<Phone
			src="screens/raw/03_ar_laser_tape_live.png"
			height={phoneH}
			rotateY={lerp(L ? -34 : -26, L ? -12 : -8, tilt)}
			rotateX={lerp(18, 6, tilt)}
			rotateZ={lerp(4, 0, tilt)}
			glow={0.3 + 0.7 * bIn}
		>
			{/* Laser across the floor to the armchair, in screenshot coordinates. */}
			<Laser
				a={{x: 92, y: 1102}}
				b={{x: 548, y: 1032}}
				draw={draw}
				anchorA={aIn}
				anchorB={bIn}
				flash={flash > 0 && flash < 1 ? flash : 0}
				label="2.42 m"
				sub={`7' 11 1/4"`}
				labelIn={label}
			/>
		</Phone>
	);

	return (
		<AbsoluteFill style={{opacity: (1 - exit * 0.9) * prog(t, 0, 0.45)}}>
			<PhotoBackground src="bg/bg_living_tv_wall.jpg" blur={18} dim={0.6} scale={1.15 + zoom * 0.05} />
			{/* Phone: rises in, settles its tilt, then slowly pushes into the room. Exits upward (parallax into the blueprint). */}
			<AbsoluteFill
				style={{
					alignItems: 'center',
					justifyContent: 'center',
					translate: L ? `${360 + (1 - enter) * 160}px ${(1 - enter) * 420 - exit * 700}px` : `0px ${110 + (1 - enter) * 520 - exit * 900}px`,
					scale: String(lerp(0.82, 1, enter) * (1 + zoom * 0.07)),
				}}
			>
				{phone}
			</AbsoluteFill>

			{/* Copy */}
			<div
				style={{
					position: 'absolute',
					left: L ? 140 : 90,
					top: L ? 250 : 170,
					width: L ? 760 : 900,
					display: 'flex',
					flexDirection: 'column',
					gap: 26,
					opacity: 1 - exit,
					translate: `${-exit * 80}px 0`,
				}}
			>
				<SpatialLabel size={L ? 22 : 26} color={C.lime}>
					01 · 3D AR Laser Tape
				</SpatialLabel>
				<Reveal text="Lock the laser with your *voice.*" at={0.25} size={L ? 84 : 92} family={F.sans} />
			</div>

			{/* Voice pin callout */}
			<div
				style={{
					position: 'absolute',
					left: L ? 150 : 70,
					top: L ? 640 : 1655,
					display: 'flex',
					flexDirection: L ? 'column' : 'row',
					alignItems: L ? 'flex-start' : 'center',
					gap: L ? 0 : 22,
					opacity: say * (1 - exit),
					translate: `${(1 - say) * -60}px ${(1 - say) * 30}px`,
				}}
			>
				<Glass radius={60}>
					<div style={{display: 'flex', alignItems: 'center', gap: 26, padding: '22px 34px 22px 24px'}}>
						<Orb size={L ? 84 : 80} energy={1 + heard} dot={false} />
						<div style={{display: 'flex', flexDirection: 'column', gap: 8}}>
							<div style={{fontFamily: F.sans, fontWeight: 800, fontSize: L ? 46 : 44, color: C.head, letterSpacing: '-0.02em'}}>
								Say <span style={{color: C.lime}}>“Mark”</span>
							</div>
							<Waveform height={34} bars={14} width={6} level={0.25 + 0.75 * heard * (1 - prog(t, B.anchorB + 0.3, B.anchorB + 0.9))} />
						</div>
					</div>
				</Glass>
				<div style={{marginTop: L ? 18 : 0, marginLeft: L ? 8 : 0, scale: String(0.7 + 0.3 * locked), opacity: locked, transformOrigin: 'left center'}}>
					<LimePill size={L ? 26 : 24} mono glow={0.5}>
						✓ Pinned · anti-shake lock
					</LimePill>
				</div>
			</div>
		</AbsoluteFill>
	);
};
