import { BrowserModule } from '@angular/platform-browser'
import { NgModule, Injector } from '@angular/core'
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http'
import { JwtHttpInterceptor } from './jwt-http.interceptor'
import { AppRoutingModule } from './app-routing.module'
import { AppComponent } from './app.component'
import { SocialLoginModule, SocialAuthServiceConfig } from "angularx-social-login"
import { GoogleLoginProvider } from "angularx-social-login"
import { LoginComponent } from './login/login.component'
import { CheckoutComponent } from './checkout/checkout.component'
import { ConfigureComponent } from './configure/configure.component'
import { MainComponent } from './main/main.component'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'
import { ReactiveFormsModule } from '@angular/forms'
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'
import { DomainComponent } from './domain/domain.component'
import { InitialiseService } from './initialise.service'
import { Initialisation } from './server-api.service'
import { AlertComponent } from './alert/alert.component'
import { RegisterComponent } from './register/register.component'
import { MatSortModule } from '@angular/material/sort'
import { TermsConditionsPrivacyComponent } from './terms-conditions-privacy/terms-conditions-privacy.component'

/**
 * Override initialise to enable loading configuration form server.
 */
function setupLoginProvider(loginProvider: any, initialiseService: InitialiseService) {
	const initialise = loginProvider.initialize
	loginProvider.initialize = () => new Promise((resolve, reject) => {
		initialiseService.initialisation().subscribe((initialisation: Initialisation) => {

			// Determine if authentication required
			if (!initialisation.isAuthenticationRequired) {
				reject(new Error("No authentication required"))
				return
			}

			// Authentication required (so setup)
			loginProvider.clientId = initialisation.googleClientId
			initialise.call(loginProvider).then((result) => resolve(result)).catch(reject)
		}, reject)
	})
	return loginProvider
}

export function provideAuthServiceConfig(initialiseService: InitialiseService): SocialAuthServiceConfig {
	return {
		autoLogin: false,
		providers: [{
			id: GoogleLoginProvider.PROVIDER_ID,
			provider: setupLoginProvider(new GoogleLoginProvider("TO BE SETUP"), initialiseService)
		}]
	} as SocialAuthServiceConfig
}

@NgModule({
	declarations: [
		AppComponent,
		LoginComponent,
		CheckoutComponent,
		ConfigureComponent,
		MainComponent,
		DomainComponent,
		AlertComponent,
		RegisterComponent,
		TermsConditionsPrivacyComponent,
	],
	imports: [
		BrowserModule,
		BrowserAnimationsModule,
		HttpClientModule,
		ReactiveFormsModule,
		NgbModule,
		SocialLoginModule,
		AppRoutingModule,
		MatSortModule,
	],
	providers: [{
		provide: 'SocialAuthServiceConfig',
		useFactory: provideAuthServiceConfig,
		deps: [InitialiseService]
	}, {
		provide: HTTP_INTERCEPTORS,
		useClass: JwtHttpInterceptor,
		multi: true,
		deps: [Injector]
	}],
	bootstrap: [AppComponent]
})
export class AppModule { }
