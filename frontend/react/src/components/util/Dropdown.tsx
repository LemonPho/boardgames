import { useUIContext } from '../UIContext'

interface DropdownProps {
  id: string
  children: React.ReactNode
}

export default function Dropdown({ id, children }: DropdownProps) {
  const { activePanel } = useUIContext()
  const isOpen = activePanel === id
  console.log(activePanel + " == " + id);

  if (!isOpen) return null

  return (
    <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-xl shadow-lg py-1 z-50">
      {children}
    </div>
  )
}