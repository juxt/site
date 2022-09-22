export const saveColourBlindMode = (colourBlindMode: boolean) => {
  localStorage.setItem('colourBlindMode', colourBlindMode.toString())
}

export const loadColourBlindMode = () => {
  const colourBlindMode = localStorage.getItem('colourBlindMode')
  return colourBlindMode ? colourBlindMode === 'true' : false
}
