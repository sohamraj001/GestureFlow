const fs = require('fs');
const path = require('path');
const https = require('https');

const TOKEN = process.argv[2];
const REPO = 'sohamraj001/GestureFlow';
const BRANCH = 'main';

if (!TOKEN) {
    console.error('Error: Please provide your GitHub Personal Access Token.');
    console.log('Usage: node push_to_github.js <YOUR_TOKEN>');
    process.exit(1);
}

async function uploadFile(filePath, repoPath) {
    const content = fs.readFileSync(filePath).toString('base64');
    const data = JSON.stringify({
        message: `Upload ${repoPath}`,
        content: content,
        branch: BRANCH
    });

    const options = {
        hostname: 'api.github.com',
        path: `/repos/${REPO}/contents/${repoPath}`,
        method: 'PUT',
        headers: {
            'Authorization': `token ${TOKEN}`,
            'User-Agent': 'Node.js Script',
            'Content-Type': 'application/json',
            'Content-Length': data.length
        }
    };

    return new Promise((resolve, reject) => {
        const req = https.request(options, (res) => {
            if (res.statusCode === 201 || res.statusCode === 200) {
                console.log(`Successfully uploaded: ${repoPath}`);
                resolve();
            } else {
                let body = '';
                res.on('data', d => body += d);
                res.on('end', () => reject(`Failed to upload ${repoPath}: ${res.statusCode} ${body}`));
            }
        });
        req.on('error', reject);
        req.write(data);
        req.end();
    });
}

function getAllFiles(dirPath, arrayOfFiles) {
    const files = fs.readdirSync(dirPath);
    arrayOfFiles = arrayOfFiles || [];

    files.forEach(function(file) {
        if (fs.statSync(dirPath + "/" + file).isDirectory()) {
            if (file !== 'node_modules' && file !== '.git' && file !== '.gradle' && file !== 'build') {
                arrayOfFiles = getAllFiles(dirPath + "/" + file, arrayOfFiles);
            }
        } else {
            arrayOfFiles.push(path.join(dirPath, "/", file));
        }
    });

    return arrayOfFiles;
}

async function pushProject() {
    const files = getAllFiles('.');
    console.log(`Found ${files.length} files. Starting upload...`);

    for (const file of files) {
        const repoPath = path.relative('.', file).replace(/\\/g, '/');
        try {
            await uploadFile(file, repoPath);
        } catch (err) {
            console.error(err);
        }
    }
    console.log('Push complete!');
}

pushProject();
