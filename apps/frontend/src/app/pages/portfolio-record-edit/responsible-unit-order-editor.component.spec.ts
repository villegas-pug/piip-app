import { ResponsibleUnitOrderEditorComponent } from './responsible-unit-order-editor.component';

describe('ResponsibleUnitOrderEditorComponent', () => {
  it('emits deterministic order changes when moving an item', () => {
    const component = new ResponsibleUnitOrderEditorComponent();
    component.selectedIds = [10, 20, 30];
    const emitted: number[][] = [];
    component.selectedIdsChange.subscribe((ids) => emitted.push(ids));

    component.move(2, -1);

    expect(emitted).toEqual([[10, 30, 20]]);
  });

  it('does not emit an empty duplicate selection from a valid list', () => {
    const component = new ResponsibleUnitOrderEditorComponent();
    component.selectedIds = [10, 20];
    const emitted: number[][] = [];
    component.selectedIdsChange.subscribe((ids) => emitted.push(ids));

    component.remove(0);

    expect(emitted).toEqual([[20]]);
  });
});
