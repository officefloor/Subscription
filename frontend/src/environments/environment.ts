/* 
 * This file can be replaced during build by using the `fileReplacements` array.
 * `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
 * The list of file replacements can be found in `angular.json`.
 */

import 'zone.js/dist/zone-error' // easier development debugging

export const environment = {
    production: false,
    serverUrl: window.location.href.startsWith( 'http://localhost:4200' ) ? 'http://localhost:8080' : ''
}
