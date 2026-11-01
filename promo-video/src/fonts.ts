import {loadFont} from '@remotion/fonts';
import {staticFile} from 'remotion';
import {F} from './lib';

// The app's own bundled fonts (copied from app/src/main/res/font). loadFont holds each frame until ready.
const jakarta: Array<[string, string, 'normal' | 'italic']> = [
	['400', 'plus_jakarta_sans_400', 'normal'],
	['500', 'plus_jakarta_sans_500', 'normal'],
	['600', 'plus_jakarta_sans_600', 'normal'],
	['700', 'plus_jakarta_sans_700', 'normal'],
	['800', 'plus_jakarta_sans_800', 'normal'],
	['700', 'plus_jakarta_sans_italic_700', 'italic'],
	['800', 'plus_jakarta_sans_italic_800', 'italic'],
];
const mono: Array<[string, string]> = [
	['400', 'jetbrains_mono_400'],
	['500', 'jetbrains_mono_500'],
	['600', 'jetbrains_mono_600'],
	['700', 'jetbrains_mono_700'],
];

for (const [weight, file, style] of jakarta) {
	loadFont({family: F.sans, url: staticFile(`fonts/${file}.ttf`), weight, style});
}
for (const [weight, file] of mono) {
	loadFont({family: F.mono, url: staticFile(`fonts/${file}.ttf`), weight});
}
