import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { JwtHttpInterceptor } from './jwt-http.interceptor';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SocialLoginModule, AuthServiceConfig } from "angularx-social-login";
import { GoogleLoginProvider, FacebookLoginProvider, LinkedInLoginProvider } from "angularx-social-login";
import { LoginComponent } from './login/login.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { ConfigureComponent } from './configure/configure.component';
import { MainComponent } from './main/main.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ReactiveFormsModule } from '@angular/forms';

let config = new AuthServiceConfig( [
    {
        id: GoogleLoginProvider.PROVIDER_ID,
        provider: new GoogleLoginProvider( "443132781504-19vekci7r4t2qvqpbg9q1s32kjnp1c7t.apps.googleusercontent.com" )
    }
] );

export function provideConfig() {
    return config;
}

@NgModule( {
    declarations: [
        AppComponent,
        LoginComponent,
        CheckoutComponent,
        ConfigureComponent,
        MainComponent
    ],
    imports: [
        BrowserModule,
        HttpClientModule,
        ReactiveFormsModule,
        NgbModule,
        SocialLoginModule,
        AppRoutingModule
    ],
    providers: [{
        provide: AuthServiceConfig,
        useFactory: provideConfig
    }, {
        provide: HTTP_INTERCEPTORS,
        useClass: JwtHttpInterceptor,
        multi: true
    }],
    bootstrap: [AppComponent]
} )
export class AppModule { }
