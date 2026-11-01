import React from 'react';
import {AbsoluteFill, Sequence, useCurrentFrame, useVideoConfig} from 'remotion';
import {AppIcon} from './components/AppIcon';
import {Grain, Vignette} from './components/Background';
import {C, easeIn, F, prog, TL} from './lib';
import {S1Hook} from './scenes/S1Hook';
import {S2Tape} from './scenes/S2Tape';
import {S3Blueprint} from './scenes/S3Blueprint';
import {S4Advisor} from './scenes/S4Advisor';
import {S5Outro} from './scenes/S5Outro';
import './fonts';

/** Small persistent brand mark — present from the hook until the outro takes over the brand. */
const Wordmark: React.FC = () => {
	const frame = useCurrentFrame();
	const {fps, width, height} = useVideoConfig();
	const L = width > height;
	const t = frame / fps;
	const o = prog(t, 0.35, 0.9) * (1 - prog(t, TL.s5.start - 0.3, TL.s5.start, easeIn));
	return (
		<div
			style={{
				position: 'absolute',
				left: L ? 72 : 80,
				top: L ? 56 : 96,
				display: 'flex',
				alignItems: 'center',
				gap: 16,
				opacity: o,
				translate: `0 ${(1 - prog(t, 0.35, 0.9)) * -16}px`,
			}}
		>
			<AppIcon size={L ? 54 : 64} />
			<div style={{fontFamily: F.sans, fontWeight: 800, fontSize: L ? 30 : 34, color: C.head, letterSpacing: '-0.02em'}}>
				MeasurePulse <span style={{color: C.lime}}>AI</span>
			</div>
		</div>
	);
};

/**
 * 18 s autoplay store banner. One component renders both 1920×1080 (Google Play) and 1080×1920 (App Store / Shorts);
 * each scene lays itself out from the canvas aspect. Scenes overlap ~0.4 s so nothing ever cuts to black.
 */
export const MeasurePulseBanner: React.FC = () => {
	const {fps} = useVideoConfig();
	return (
		<AbsoluteFill style={{backgroundColor: C.nadir}}>
			<Sequence name="S1 Hook · AI orb" durationInFrames={Math.round(TL.s1.end * fps)}>
				<S1Hook />
			</Sequence>
			<Sequence name="S2 AR laser tape" from={Math.round(TL.s2.start * fps)} durationInFrames={Math.round((TL.s2.end - TL.s2.start) * fps)}>
				<S2Tape />
			</Sequence>
			<Sequence name="S3 Blueprint + box fit" from={Math.round(TL.s3.start * fps)} durationInFrames={Math.round((TL.s3.end - TL.s3.start) * fps)}>
				<S3Blueprint />
			</Sequence>
			<Sequence name="S4 AI advisor + tools" from={Math.round(TL.s4.start * fps)} durationInFrames={Math.round((TL.s4.end - TL.s4.start) * fps)}>
				<S4Advisor />
			</Sequence>
			<Sequence name="S5 Widgets + CTA" from={Math.round(TL.s5.start * fps)} durationInFrames={Math.round((TL.s5.end - TL.s5.start) * fps)}>
				<S5Outro />
			</Sequence>
			<Wordmark />
			<Vignette strength={0.5} />
			<Grain opacity={0.07} />
		</AbsoluteFill>
	);
};
