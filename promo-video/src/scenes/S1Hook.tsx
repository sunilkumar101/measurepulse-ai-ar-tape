import React from 'react';
import {AbsoluteFill, useCurrentFrame, useVideoConfig} from 'remotion';
import {DotGrid, PhotoBackground} from '../components/Background';
import {Glass, SpatialLabel} from '../components/Glass';
import {Orb, Waveform} from '../components/Orb';
import {Reveal} from '../components/Reveal';
import {C, easeIn, F, HEAVY, prog, sp, TL} from '../lib';

const B = TL.s1;

/** 0–3 s · Hook: the AI orb listens inside a floating frosted glass pill over the twilight villa. */
export const S1Hook: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps, width, height} = useVideoConfig();
	const L = width > height;
	const t = frame / fps;

	// Frame 0 is the thumbnail: photo, orb and glass are already present; only the words arrive.
	const pill = sp(frame, fps, B.orbIn, HEAVY);
	const exit = prog(t, B.exit, B.exit + 0.4, easeIn);
	const wave = prog(t, B.waveLive, B.waveLive + 0.5);
	const push = 1.08 + t * 0.03;

	const orbSize = L ? 190 : 230;
	const textSize = L ? 84 : 104;

	return (
		<AbsoluteFill style={{opacity: 1 - exit}}>
			<PhotoBackground src="bg/bg_villa_cliff.jpg" scale={push + exit * 0.08} y={-t * 6} dim={0.42} />
			<DotGrid opacity={0.1} />
			<AbsoluteFill style={{alignItems: 'center', justifyContent: 'center', scale: String(1 + exit * 0.06)}}>
				<div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: L ? 34 : 44}}>
					<SpatialLabel size={L ? 22 : 26} color={C.lime} style={{opacity: 0.55 + 0.45 * pill}}>
						Spatial AI · On-device
					</SpatialLabel>
					<Glass
						radius={L ? 72 : 64}
						style={{
							scale: String(0.94 + 0.06 * pill),
							translate: `0 ${(1 - pill) * 30}px`,
							rotate: `${-2.5 + 2.5 * pill}deg`,
						}}
					>
						<div
							style={{
								display: 'flex',
								flexDirection: L ? 'row' : 'column',
								alignItems: 'center',
								gap: L ? 44 : 40,
								padding: L ? '46px 72px 46px 52px' : '64px 64px 60px',
							}}
						>
							<div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 22}}>
								<Orb size={orbSize} energy={0.7 + 0.6 * wave} />
								<Waveform height={L ? 46 : 54} bars={13} width={L ? 7 : 8} level={0.15 + 0.85 * wave} />
							</div>
							<div style={{display: 'flex', flexDirection: 'column', gap: 6, width: L ? 1120 : 840}}>
								<Reveal text="Measure Any Room in 3D" at={B.line1} size={textSize} family={F.sans} align={L ? 'left' : 'center'} />
								<Reveal text="with *Spatial* *AI*" at={B.line2} size={textSize} family={F.sans} align={L ? 'left' : 'center'} />
							</div>
						</div>
					</Glass>
				</div>
			</AbsoluteFill>
		</AbsoluteFill>
	);
};
