const { exec } = require('child_process');
const fs = require('fs');

console.log('🔨 Building TypeScript web viewer...');

exec('npx tsc', (error, stdout, stderr) => {
    if (error) {
        console.error('Build failed:', error);
        return;
    }

    if (stderr) {
        console.error('TypeScript errors:', stderr);
        return;
    }

    console.log('✅ TypeScript build successful!');
    console.log('📁 Files output to: dist/');

    // Copy HTML file to dist
    fs.copyFileSync('src/index.html', 'dist/index.html');
    console.log('📄 HTML file copied to dist/');
});