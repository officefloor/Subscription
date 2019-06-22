
// Fix for Zone promise overwritten by vendors.js
declare let window: any
Promise = window.FIX_ZONE_PROMISE

import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if ( environment.production ) {
    enableProdMode();
}

platformBrowserDynamic().bootstrapModule( AppModule )
    .catch( err => console.error( err ) );
