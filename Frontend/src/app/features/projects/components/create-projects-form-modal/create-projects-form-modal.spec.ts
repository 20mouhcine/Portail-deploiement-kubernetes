import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateProjectsFormModal } from './create-projects-form-modal';

describe('CreateProjectsFormModal', () => {
  let component: CreateProjectsFormModal;
  let fixture: ComponentFixture<CreateProjectsFormModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateProjectsFormModal],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateProjectsFormModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
