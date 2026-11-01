import React from 'react';
import {Composition, Folder, Still} from 'remotion';
import {MeasurePulseBanner} from './Banner';
import {AppIcon} from './components/AppIcon';
import {FeatureGraphic} from './stills/FeatureGraphic';
import {StoreShot} from './stills/StoreShot';

const HiResIcon: React.FC = () => <AppIcon size={512} radius={0} glow={false} />;

export const RemotionRoot: React.FC = () => {
	return (
		<>
			<Folder name="AutoplayBanner">
				{/* Google Play promo — 1920×1080 landscape */}
				<Composition id="MeasurePulseAutoplayBanner" component={MeasurePulseBanner} durationInFrames={1080} fps={60} width={1920} height={1080} />
				{/* App Store / Shorts — 1080×1920 vertical */}
				<Composition id="MeasurePulseAutoplayBannerVertical" component={MeasurePulseBanner} durationInFrames={1080} fps={60} width={1080} height={1920} />
			</Folder>
			<Folder name="PlayStoreGlassScreenshots">
				<Still
					id="Shot01ArLaserTape"
					component={StoreShot}
					width={1080}
					height={1920}
					defaultProps={{
						index: '01',
						label: '3D AR Laser Tape',
						headline: 'Say “Mark”. *Measured.*',
						sub: 'Voice & volume-key pin with an anti-shake lock.',
						shot: 'screens/raw/03_ar_laser_tape_live.png',
						bg: 'bg/bg_living_tv_wall.jpg',
						callout: 'Line · Room · 3D Box · Angle',
						laser: true,
					}}
				/>
				<Still
					id="Shot02RoomBlueprint"
					component={StoreShot}
					width={1080}
					height={1920}
					defaultProps={{
						index: '02',
						label: 'Room Blueprint',
						headline: 'Blueprints in *one sweep.*',
						sub: '2D CAD floorplan, wall areas and a scaled A4 PDF.',
						shot: 'screens/raw/04_room_blueprint.png',
						bg: 'bg/bg_city_lounge.jpg',
						callout: '29.1 m² · 4 walls · 3.10 m',
						laser: false,
					}}
				/>
				<Still
					id="Shot03AiRoomAdvisor"
					component={StoreShot}
					width={1080}
					height={1920}
					defaultProps={{
						index: '03',
						label: 'Help With AI',
						headline: 'Know exactly what to *buy.*',
						sub: 'Tiles, paint litres, AC tonnage and TV size — instantly.',
						shot: 'screens/raw/08_ai_room_advisor.png',
						bg: 'bg/bg_ocean_room_scan.jpg',
						callout: '89 tiles · 13.2 L paint',
						laser: false,
					}}
				/>
				<Still
					id="Shot04SpatialHub"
					component={StoreShot}
					width={1080}
					height={1920}
					defaultProps={{
						index: '04',
						label: 'Spatial Command Hub',
						headline: 'Every room, *one hub.*',
						sub: 'Metric or imperial, down to 1/32 of an inch.',
						shot: 'screens/raw/02_home_hub_pro.png',
						bg: 'bg/bg_villa_cliff.jpg',
						callout: "m · cm · mm · ft-in 1/32\"",
						laser: false,
					}}
				/>
				<Still
					id="Shot05Caliper"
					component={StoreShot}
					width={1080}
					height={1920}
					defaultProps={{
						index: '05',
						label: 'Vernier Caliper',
						headline: 'A caliper in your *pocket.*',
						sub: 'Drag the jaws, gauge thread pitch, calibrate with a card.',
						shot: 'screens/raw/06_caliper.png',
						bg: 'bg/bg_city_lounge.jpg',
						callout: '±0.09 mm · ISO bolt match',
						laser: false,
					}}
				/>
				<Still
					id="Shot06LevelWallLaser"
					component={StoreShot}
					width={1080}
					height={1920}
					defaultProps={{
						index: '06',
						label: 'Bubble Level + Wall Laser',
						headline: 'Hang it *dead level.*',
						sub: 'Bullseye level, roof pitch and an AR wall laser line.',
						shot: 'screens/raw/07_level_wall_laser.png',
						bg: 'bg/bg_living_tv_wall.jpg',
						callout: '±0.1° beep + haptic',
						laser: false,
					}}
				/>
				<Still
					id="Shot07ProjectsGallery"
					component={StoreShot}
					width={1080}
					height={1920}
					defaultProps={{
						index: '07',
						label: 'Saved Projects',
						headline: 'Every scan, *saved.*',
						sub: 'Search, filter and re-share your PDFs in one tap.',
						shot: 'screens/raw/09_projects_gallery_pro.png',
						bg: 'bg/bg_villa_pool.jpg',
						callout: 'Stored on your device',
						laser: false,
					}}
				/>
				<Still
					id="Shot08GlassWidgets"
					component={StoreShot}
					width={1080}
					height={1920}
					defaultProps={{
						index: '08',
						label: 'Home-Screen Widgets',
						headline: 'Measure from your *home screen.*',
						sub: 'Three frosted glass widgets, one tap to every tool.',
						shot: 'screens/raw/11_home_screen_widgets.png',
						bg: 'bg/bg_villa_pool.jpg',
						callout: '4×2 · 2×2 · 4×1 widgets',
						laser: false,
					}}
				/>
				<Still id="FeatureGraphic" component={FeatureGraphic} width={1024} height={500} />
				<Still id="HiResIcon" component={HiResIcon} width={512} height={512} />
			</Folder>
		</>
	);
};
