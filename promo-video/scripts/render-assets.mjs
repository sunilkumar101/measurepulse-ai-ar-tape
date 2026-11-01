// Renders the Play Store stills into ../playstore_assets/ (bundle once, render each still at full size).
import {bundle} from '@remotion/bundler';
import {renderStill, selectComposition} from '@remotion/renderer';
import fs from 'node:fs';
import path from 'node:path';

const OUT = path.resolve('..', 'playstore_assets');
const jobs = [
	['Shot01ArLaserTape', 'screenshots/01_ar_laser_tape.png'],
	['Shot02RoomBlueprint', 'screenshots/02_room_blueprint.png'],
	['Shot03AiRoomAdvisor', 'screenshots/03_ai_room_advisor.png'],
	['Shot04SpatialHub', 'screenshots/04_spatial_hub.png'],
	['Shot05Caliper', 'screenshots/05_vernier_caliper.png'],
	['Shot06LevelWallLaser', 'screenshots/06_level_wall_laser.png'],
	['Shot07ProjectsGallery', 'screenshots/07_projects_gallery.png'],
	['Shot08GlassWidgets', 'screenshots/08_glass_widgets.png'],
	['FeatureGraphic', 'feature_graphic_1024x500.png'],
	['HiResIcon', 'icon_512x512.png'],
];

const only = process.argv.slice(2);
const serveUrl = await bundle({entryPoint: path.resolve('src/index.ts')});
for (const [id, file] of jobs) {
	if (only.length && !only.includes(id)) continue;
	const composition = await selectComposition({serveUrl, id});
	const output = path.join(OUT, file);
	fs.mkdirSync(path.dirname(output), {recursive: true});
	await renderStill({composition, serveUrl, output, imageFormat: 'png'});
	console.log(`${output}  ${composition.width}x${composition.height}  ${fs.statSync(output).size} bytes`);
}
