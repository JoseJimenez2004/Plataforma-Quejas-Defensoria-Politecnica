"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const child_process_1 = require("child_process");
const workspace_1 = require("./workspace");
const colors_1 = require("./colors");
const process_1 = require("process");
let padding;
function startCmd(name, cmd) {
    const process = (0, child_process_1.exec)(cmd);
    process.stdout.on('data', (chunk) => {
        (0, colors_1.print)(name, padding, chunk);
    });
    process.stderr.on('data', (chunk) => {
        (0, colors_1.print)(name, padding, chunk, true);
    });
}
// tslint:disable-next-line: no-shadowed-variable
function startApps(apps) {
    for (const app of apps) {
        const cmd = `ng serve ${app.name} -o`;
        (0, colors_1.print)('DEVSVR', padding, app.name + ' ' + (app.port || '4200'));
        startCmd(app.name, cmd);
    }
}
if (!(0, workspace_1.isWorkspace)()) {
    console.error('This needs to be started in the root of an Angular project!');
    process.exit(0);
}
const [, , ...filter] = process_1.argv;
const startAll = filter.length === 0;
const projects = (0, workspace_1.readProjectInfos)();
const apps = projects.filter((p) => p.projectType === 'application' &&
    !p.name.endsWith('-e2e') &&
    (startAll || filter.includes(p.name)));
padding = apps.reduce((acc, p) => Math.max(acc, p.name.length), 0);
padding = Math.max(6, padding);
startApps(apps);
//# sourceMappingURL=mf-dev-server.js.map