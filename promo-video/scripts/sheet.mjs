// Contact sheets for the review loop: one still per timeline beat (+0.3 s so motion has settled).
// Usage: node scripts/sheet.mjs [CompositionId] [--times 3.2,3.4]   → out/sheets/<id>/t_<sec>.png
import {bundle} from '@remotion/bundler';
import {renderStill, selectComposition} from '@remotion/renderer';
import fs from 'node:fs';
import path from 'node:path';

const tl = JSON.parse(fs.readFileSync('src/timeline.json', 'utf8'));
const args = process.argv.slice(2);
const ids = args.filter((a) => !a.startsWith('--') && !/^[\d.,]+$/.test(a));
const timesArg = args.includes('--times') ? args[args.indexOf('--times') + 1] : null;
const compIds = ids.length ? ids : ['MeasurePulseAutoplayBanner', 'MeasurePulseAutoplayBannerVertical'];

let times;
if (timesArg) {
	times = timesArg.split(',').map(Number);
} else {
	times = [0];
	for (const [k, v] of Object.entries(tl)) {
		if (!k.startsWith('s') || typeof v !== 'object') continue;
		for (const [beat, sec] of Object.entries(v)) {
			if (beat === 'start' || beat === 'end') continue;
			times.push(+(v.start + sec + 0.3).toFixed(2));
		}
	}
	times.push(tl.duration - 0.05);
	times = [...new Set(times)].filter((t) => t >= 0 && t < tl.duration).sort((a, b) => a - b);
}

const serveUrl = await bundle({entryPoint: path.resolve('src/index.ts')});
for (const id of compIds) {
	const composition = await selectComposition({serveUrl, id});
	const dir = path.join('out', 'sheets', id);
	fs.mkdirSync(dir, {recursive: true});
	for (const t of times) {
		const frame = Math.min(composition.durationInFrames - 1, Math.round(t * composition.fps));
		const output = path.join(dir, `t_${t.toFixed(2)}.png`);
		await renderStill({composition, serveUrl, output, frame, scale: 0.5});
		console.log(output);
	}
}
