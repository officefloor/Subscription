import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TermsConditionsPrivacyComponent } from './terms-conditions-privacy.component';

describe('TermsConditionsPrivacyComponent', () => {
  let component: TermsConditionsPrivacyComponent;
  let fixture: ComponentFixture<TermsConditionsPrivacyComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TermsConditionsPrivacyComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TermsConditionsPrivacyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
