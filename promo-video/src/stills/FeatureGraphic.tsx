import React from 'react';
import {AbsoluteFill} from 'remotion';
import {AppIcon} from '../components/AppIcon';
import {DotGrid, PhotoBackground, Vignette} from '../components/Background';
import {GlassPill, Headline} from '../components/Glass';
import {Laser} from '../components/Laser';
import {Phone} from '../components/Phone';
import {C, F} from '../lib';
import '../fonts';

/** 1024×500 Google Play feature graphic. Key content stays inside the centre safe zone (Play may crop edges). */
export const FeatureGraphic: React.FC = () => (
	<AbsoluteFill style={{backgroundColor: C.nadir}}>
		<PhotoBackground src="bg/bg_villa_cliff.jpg" dim={0.5} scale={1.05} y={-40} />
		<AbsoluteFill style={{background: 'linear-gradient(90deg, rgba(17,19,36,0.92) 0%, rgba(17,19,36,0.7) 48%, rgba(17,19,36,0.1) 100%)'}} />
		<DotGrid opacity={0.08} />
		<Vignette strength={0.4} />
		<div style={{position: 'absolute', left: 64, top: 70, width: 600, display: 'flex', flexDirection: 'column', gap: 18}}>
			<div style={{display: 'flex', alignItems: 'center', gap: 18}}>
				<AppIcon size={78} />
				<div style={{fontFamily: F.sans, fontWeight: 800, fontSize: 46, color: C.head, letterSpacing: '-0.03em'}}>
					MeasurePulse <span style={{color: C.lime}}>AI</span>
				</div>
			</div>
			<Headline text="Measure any room in 3D with *Spatial AI*" size={50} />
			<div style={{display: 'flex', gap: 10, flexWrap: 'wrap', marginTop: 6}}>
				<GlassPill size={17}>AR Laser Tape</GlassPill>
				<GlassPill size={17}>Room Blueprint</GlassPill>
				<GlassPill size={17}>Caliper</GlassPill>
				<GlassPill size={17}>Level</GlassPill>
			</div>
		</div>
		<div style={{position: 'absolute', right: 70, top: 28}}>
			<Phone src="screens/raw/03_ar_laser_tape_live.png" height={560} rotateY={-22} rotateX={10} rotateZ={3} glow={0.6}>
				<Laser a={{x: 92, y: 1102}} b={{x: 548, y: 1032}} draw={1} anchorA={1} anchorB={1} label="2.42 m" labelIn={1} />
			</Phone>
		</div>
	</AbsoluteFill>
);
