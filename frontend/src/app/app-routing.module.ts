import { NgModule } from '@angular/core'
import { Routes, RouterModule } from '@angular/router'
import { ConfigureComponent } from './configure/configure.component'
import { MainComponent } from './main/main.component'
import { DomainComponent } from './domain/domain.component'
import { TermsConditionsPrivacyComponent } from './terms-conditions-privacy/terms-conditions-privacy.component'

const routes: Routes = [
    { path: 'configure', component: ConfigureComponent },
    { path: 'domain/:domain', component: DomainComponent },
    { path: 'terms_conditions_privacy', component: TermsConditionsPrivacyComponent, data: { insecure: true } },
    { path: '', component: MainComponent }
];

@NgModule( {
    imports: [RouterModule.forRoot(routes, { relativeLinkResolution: 'legacy' })],
    exports: [RouterModule]
} )
export class AppRoutingModule { }
