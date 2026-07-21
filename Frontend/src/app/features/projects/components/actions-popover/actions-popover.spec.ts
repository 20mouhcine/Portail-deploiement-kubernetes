import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActionsPopover } from './actions-popover';

describe('ActionsPopover', () => {
  let component: ActionsPopover;
  let fixture: ComponentFixture<ActionsPopover>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActionsPopover],
    }).compileComponents();

    fixture = TestBed.createComponent(ActionsPopover);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
