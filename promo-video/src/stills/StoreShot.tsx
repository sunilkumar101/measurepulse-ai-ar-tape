import React from 'react';
import {AbsoluteFill} from 'remotion';
import {DotGrid, PhotoBackground, TwilightGradient, Vignette} from '../components/Background';
import {Glass, Headline, SpatialLabel} from '../components/Glass';
import {Laser} from '../components/Laser';
import {Orb} from '../components/Orb';
import {Phone} from '../components/Phone';
import {C, F} from '../lib';
import '../fonts';

export type StoreShotProps = {
	readonly index: string;
	readonly label: string;
	readonly headline: string;
	readonly sub: string;
	readonly shot: string;
	readonly bg: string;
	readonly callout: string;
	readonly laser: boolean;
};

/** 1080×1920 framed Play Store screenshot: big benefit headline + the real phone capture in a glass-lit frame. */
export const StoreShot: React.FC<StoreShotProps> = ({index, label, headline, sub, shot, bg, callout, laser}) => (
	<AbsoluteFill style={{backgroundColor: C.nadir}}>
		<TwilightGradient />
		<PhotoBackground src={bg} blur={26} dim={0.62} scale={1.2} opacity={0.75} />
		<DotGrid opacity={0.1} />
		<Vignette strength={0.45} />

		<div style={{position: 'absolute', left: 84, top: 112, width: 912, display: 'flex', flexDirection: 'column', gap: 22}}>
			<SpatialLabel size={26} color={C.lime}>
				{index} · {label}
			</SpatialLabel>
			<Headline text={headline} size={106} />
			<div style={{fontFamily: F.sans, fontWeight: 500, fontSize: 38, lineHeight: 1.3, color: C.ice}}>{sub}</div>
			<div style={{marginTop: 6}}>
				<Glass radius={60} tint="rgba(18,21,42,0.82)" style={{display: 'inline-block'}}>
					<div style={{display: 'flex', alignItems: 'center', gap: 18, padding: '14px 30px 14px 16px'}}>
						<Orb size={54} dot={false} />
						<div style={{fontFamily: F.mono, fontWeight: 700, fontSize: 30, color: C.head, whiteSpace: 'nowrap'}}>{callout}</div>
					</div>
				</Glass>
			</div>
		</div>

		{/* The phone runs off the bottom edge on purpose: the screen content, not the nav bar, is the point. */}
		<div style={{position: 'absolute', left: 0, right: 0, top: 760, display: 'flex', justifyContent: 'center'}}>
			<Phone src={shot} height={1260} rotateY={-7} rotateX={5} glow={0.45}>
				{laser ? (
					<Laser a={{x: 92, y: 1102}} b={{x: 548, y: 1032}} draw={1} anchorA={1} anchorB={1} label="2.42 m" sub={`7' 11 1/4"`} labelIn={1} />
				) : null}
			</Phone>
		</div>

	</AbsoluteFill>
);
