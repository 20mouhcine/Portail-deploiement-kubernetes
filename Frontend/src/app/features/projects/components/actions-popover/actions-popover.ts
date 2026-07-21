import { Component, output, signal, HostListener, ElementRef, inject } from '@angular/core';

@Component({
  selector: 'app-actions-popover',
  standalone: true,
  templateUrl: './actions-popover.html',
})
export class ActionsPopover {
  edit = output<void>();
  deleteAction = output<void>();

  isOpen = signal(false);
  private elementRef = inject(ElementRef);

  toggle(event: Event): void {
    event.stopPropagation();
    this.isOpen.update(v => !v);
  }

  onEdit(event: Event): void {
    event.stopPropagation();
    this.isOpen.set(false);
    this.edit.emit();
  }

  onDelete(event: Event): void {
    event.stopPropagation();
    this.isOpen.set(false);
    this.deleteAction.emit();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }
}
