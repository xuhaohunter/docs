"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"Bundle插件管理设置
set nocompatible             " be iMproved, required
filetype off                 " required
set rtp+=~/.vim/bundle/vundle/
call vundle#begin()
call vundle#rc()

Bundle 'liyatanggithub/vundle'
Bundle 'genutils'
Bundle 'lookupfile'
Bundle 'supertab'
Bundle 'taglist.vim'
Bundle 'The-NERD-tree'
Bundle 'omnicppcomplete'
Bundle 'CmdlineComplete'

call vundle#end()            " required
filetype plugin indent on    " required
" :PluginList       - lists configured plugins
" :PluginInstall    - installs plugins; append `!` to update or just :PluginUpdate
" :PluginSearch foo - searches for foo; append `!` to refresh local cache
" :PluginClean      - confirms removal of unused plugins; append `!` to auto-approve removal
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"Vim基本设置
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
set nocompatible    "去除vim一致性模式，避免以前版本的一些bug和局限
set ruler           "显示当前光标的行列信息
set nu              "显示行号
"set nowrap         "指定不折行。如果一行太长,超过屏幕宽度,则向右边延伸到屏幕外面
set scrolloff=7     "在上下移动光标时，光标的上方或下方至少会保留显示的行数
syntax on           "语法高亮
"set autochdir      "自动设置当前目录为正在编辑的目录
set nocp            "不兼容vi
set whichwrap=h,l,<,>
                    "左右移动光标到行首尾时自动换行
set showcmd         "显示基本模式输入的命令
filetype on         "自动识别文件类型
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"搜索相关设置
set wrapscan        "搜索过程在文件内部循环进行（默认）
set ignorecase      "搜索时忽略大小写
set hlsearch        "寻找匹配时高亮度显示
set incsearch       "在搜索模式下，随着搜索字符的逐个输入，实时进行字符串匹配，并对首个匹配到的字符串高亮显示
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"mapleader键设置
let mapleaader=";"
let g:mapleader=";"
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"中文支持
set fencs=utf-8,gbk "在vim打开一个文件时尝试utf-8，gbk两种编码，支持windows拷贝过来的文件的中文支持
set termencoding=utf-8
set fileformats=unix
set encoding=prc
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"TAB健设置
set shiftwidth=4    "程序中自动缩进所使用的空白长度
set tabstop=4       "定义tab所等同的空格长度
"set softtabstop=4  "TAB键和空格键的进制转换，如果打开，空白长度满足4个空格键时会转换为一个TAB
set expandtab       "设置空格代替tab健
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"自动缩进设置
set cindent         "很好的识别出C和Java等结构化程序语言，并且能用C语言的缩进格式来处理程序的缩进结构
"set smartindent    "每一行都和前一行有相同的缩进量，同时能正确的识别出花括号，当遇到右花括号（}），则取消缩进形式。
                    "此外还增加了识别C语言关键字的功能。如果一行是以#开头的，那么这种格式将会被特殊对待而不采用缩进格式
"set autoindent     "新增加的行和前一行使用相同的缩进形式
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"配对字符补全
inoremap ( ()<ESC>i
inoremap [ []<ESC>i
inoremap { {<CR>}<ESC>k$a<CR>
inoremap < <><ESC>i
inoremap ' ''<ESC>i
inoremap " ""<ESC>i
inoremap / //<ESC>a
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"括号设置
set showmatch       "设置匹配模式，显示括号配对情况
set matchtime=15    "设置键入某个闭括号时,等待时间的长短，时间单位是十分之一秒
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"行尾空格设置
highlight WhitespaceEOL ctermbg=red guibg=red
match WhitespaceEOL /\s\+$/
                    "显示行尾空格
function StripTrailingWhite()
    let winview = winsaveview()
    silent! %s/\s\+$//
    call winrestview(winview)
endfunction
autocmd BufReadPost * :call StripTrailingWhite()
                    "开启vim时去除行尾空格
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"折叠设置
set foldenable
set foldmethod=syntax
set foldlevelstart=99
                    "打开文件是默认不折叠代码
nnoremap <space> @=((foldclosed(line('.')) < 0) ? 'zc' : 'zo')<CR>
                    "绑定空格键来开关折叠
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"行／列线设置
set cursorline      "行线设置
set cursorcolumn    "列线设置
hi CursorLine  cterm=NONE   ctermbg=cyan ctermfg=white
hi CursorColumn cterm=NONE ctermbg=cyan ctermfg=white
                    "颜色设置，ctermbg为背景色，ctermfg为前景色
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"在输入模式下的光标移动
inoremap <C-h> <Left>
inoremap <C-j> <Down>
inoremap <C-k> <Up>
inoremap <C-l> <Right>
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"卷动绑定属性绑定快捷键，所有设置了卷动绑定属性的窗口将一起卷动
map <leader>b :set scrollbind<CR>
                    "绑定;b设定卷动绑定属性
map <leader>nb :set noscrollbind<CR>
                    "绑定;nb消除卷动绑定属性


"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"Vim插件设置
filetype plugin on  "允许插件运行
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"ctags cscope lookupfile工程初始化设置
map <leader>lp :!(rm cscope.files cscope.in.out cscope.out cscope.po.out .filenametags tags -rf &&echo update cscope... ...&&find . -name "*.h" -o -name "*.c" -o -name "*.cc" > cscope.files&&cscope -bkq -i cscope.files&&CSCOPE_DB=$(pwd)/cscope.out&&echo update tags ... ...&&ctags -R&&~/.vim/bash/mkfilenametags)<CR><CR>
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"Ctags设置
set tags=tags;      "ctags设置tags文件，当前路径没有tags文件时向上级路径寻找
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"cscope设置
if has("cscope")
    set cscopetag
    set csto=0
    set cst
    set csverb
    set cspc=3
    set cscopeverbose
    if filereadable("cscope.out")
    	cs add cscope.out
    else
    	let cscope_file=findfile("cscope.out",".;")
    	let cscope_pre=matchstr(cscope_file,".*/")
    	if !empty(cscope_file) && filereadable(cscope_file)
    		exe "cs add" cscope_file cscope_pre
    	endif
    endif
    "   's'   symbol: find all references to the token under cursor
    "   'g'   global: find global definition(s) of the token under cursor
    "   'c'   calls:  find all calls to the function name under cursor
    "   't'   text:   find all instances of the text under cursor
    "   'e'   egrep:  egrep search for the word under cursor
    "   'f'   file:   open the filename under cursor
    "   'i'   includes: find files that include the filename under cursor
    "   'd'   called: find functions that function under cursor calls
        nmap <C-\> :cs find s <C-R>=expand("<cword>")<CR><CR>
    "nmap <C-\>s :cs find s <C-R>=expand("<cword>")<CR><CR>
    "nmap <C-\>g :cs find g <C-R>=expand("<cword>")<CR><CR>
    "nmap <C-\>c :cs find c <C-R>=expand("<cword>")<CR><CR>
    "nmap <C-\>t :cs find t <C-R>=expand("<cword>")<CR><CR>
    "nmap <C-\>e :cs find e <C-R>=expand("<cword>")<CR><CR>
    "nmap <C-\>f :cs find f <C-R>=expand("<cfile>")<CR><CR>
    "nmap <C-\>i :cs find i ^<C-R>=expand("<cfile>")<CR>$<CR>
    "nmap <C-\>d :cs find d <C-R>=expand("<cword>")<CR><CR>
endif
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"lookupfile设置
let g:LookupFile_TagExpr = '"./.filenametags"'
let g:LookupFile_MinPatLength = 2               "最少输入2个字符才开始查找
let g:LookupFile_PreserveLastPattern = 0        "不保存上次查找的字符串
let g:LookupFile_PreservePatternHistory = 1     "保存查找历史
let g:LookupFile_AlwaysAcceptFirst = 1          "回车打开第一个匹配项目
let g:LookupFile_AllowNewFiles = 0              "不允许创建不存在的文件
" 让lookupfile插件忽略大小写
function! LookupFile_IgnoreCaseFunc(pattern)
	let _tags = &tags
	try
		let &tags = eval(g:LookupFile_TagExpr)
		let newpattern = '\c' . a:pattern
		let tags = taglist(newpattern)
	catch
		echohl ErrorMsg | echo "Exception: " . v:exception | echohl NONE
		return ""
	finally
		let &tags = _tags
	endtry

	"Show the matches for what is typed so far.
	let files = map(tags, 'v:val["filename"]')
	return files
endfunction
let g:LookupFile_LookupFunc = 'LookupFile_IgnoreCaseFunc'
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"taglist设置
let Tlist_Show_One_File=1               "只显示当前文件的tags
let Tlist_WinWidth=30                   "设置taglist宽度
let Tlist_Exit_OnlyWindow=1             "tagList窗口是最后一个窗口，则退出Vim
let Tlist_Use_Right_Window=1            "在Vim窗口右侧显示taglist窗口
"let Tlist_Auto_Open=1                  "启动vim时自动打开taglist窗口
let TlistHighlightTag=1                 "当前Tag高亮显示
let Tlist_GainFocus_On_ToggleOpen=1    	"TlistToggle打开标签列表窗口后会获焦点至于标签列表窗口
let Tlist_Close_On_Select=1    	        "选择标签或文件后是否自动关闭标签列表窗口
map <leader>ll :Tlist<CR>
                                        "绑定;ll快捷键打开关闭TagList窗口
"map <leader>ll :TlistToggle<CR>        "命令同上
"++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"NerdTree设置
let NERDTreeShowBookmarks=1             "当打开NERDTree窗口时，自动显示Bookmarks
let NERDTreeWinPos="left"               "将NERDTree 的窗口设置在 vim 窗口的左侧
let NERDTreeQuitOnOpen=1                "打开文件后关闭NerdTree窗口
let NERDTreeWinSize=30                  "设置NerdTree窗口宽度
map <leader>kk :NERDTreeToggle<CR>
                                        "绑定;kk键作为打开关闭NerdTree
autocmd bufenter * if (winnr("$") == 1 && exists("b:NERDTreeType") &&b:NERDTreeType == "primary") | q | endif
                                        "退出vim时如果打开NerdTree，一起关闭窗口
"autocmd vimenter * NERDTree
                                        "打开vim时自动打开NERDTree
