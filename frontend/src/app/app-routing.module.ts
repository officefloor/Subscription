import { NgModule } from '@angular/core'
import { Routes, RouterModule } from '@angular/router'
import { ConfigureComponent } from './configure/configure.component'
import { MainComponent } from './main/main.component'
import { DomainComponent } from './domain/domain.component'

const routes: Routes = [
    { path: 'configure', component: ConfigureComponent },
    { path: 'domain/:domain', component: DomainComponent },
    { path: '', component: MainComponent }
];

@NgModule( {
    imports: [RouterModule.forRoot( routes )],
    exports: [RouterModule]
} )
export class AppRoutingModule { }
